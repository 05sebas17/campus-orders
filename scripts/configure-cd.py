"""Run in AWS CloudShell: inspect, provision deployment support, then add Deploy.

The current pipeline and EC2 instance remain external to CloudFormation.
An existing non-project instance profile is never replaced.
"""
import argparse
import json
import subprocess
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
REGION = "us-east-1"
PIPELINE = "campus-orders-ci"
STACK = "campus-orders-ec2-cd"
ACCOUNT = "889033546125"


def add_deploy_stage(pipeline, tag_value=STACK):
    """Pure transformation; preserve Source, Build, triggers and their configuration."""
    pipeline = json.loads(json.dumps(pipeline))
    if pipeline.get("pipelineType") != "V2":
        raise ValueError("EC2 deployment requires an existing V2 pipeline")
    stages = pipeline["stages"]
    builds = [s for s in stages if any(a["actionTypeId"]["provider"] == "CodeBuild" for a in s["actions"])]
    if len(builds) != 1:
        raise ValueError("Expected one CodeBuild stage")
    actions = [a for a in builds[0]["actions"] if a["actionTypeId"]["provider"] == "CodeBuild"]
    if len(actions) != 1 or len(actions[0].get("outputArtifacts", [])) != 1:
        raise ValueError("Expected one CodeBuild output artifact")
    deploy = {
        "name": "Deploy",
        "actions": [{
            "name": "DeployEC2",
            "actionTypeId": {"category": "Deploy", "owner": "AWS", "provider": "EC2", "version": "1"},
            "runOrder": 1,
            "configuration": {
                "InstanceTagKey": "CampusOrdersDeploy", "InstanceTagValue": tag_value,
                "InstanceType": "EC2", "TargetDirectory": "/opt/campus-orders/incoming",
                "PostScript": "deploy/run-ec2.sh", "MaxBatch": "1", "MaxError": "0",
            },
            "inputArtifacts": [{"name": actions[0]["outputArtifacts"][0]["name"]}],
            "outputArtifacts": [], "region": REGION,
        }],
    }
    existing = [s for s in stages if s["name"] == "Deploy"]
    if existing:
        if existing[0] != deploy:
            raise ValueError("An existing different Deploy stage will not be overwritten")
        return pipeline
    if stages[-1]["name"] != builds[0]["name"]:
        raise ValueError("Expected Build to be the last existing stage")
    pipeline["stages"].append(deploy)
    return pipeline


def main():
    import boto3

    parser = argparse.ArgumentParser()
    parser.add_argument("mode", choices=["plan", "prepare", "enable"])
    args = parser.parse_args()
    session = boto3.Session(region_name=REGION)
    identity = session.client("sts").get_caller_identity()
    if identity["Account"] != ACCOUNT:
        raise SystemExit("Unexpected AWS account; no changes made")
    cp, ec2, cf = (session.client(n) for n in ["codepipeline", "ec2", "cloudformation"])
    pipeline = cp.get_pipeline(name=PIPELINE)["pipeline"]
    # Validate the action before creating any infrastructure.
    updated = add_deploy_stage(pipeline)
    if pipeline.get("executionMode", "SUPERSEDED") != "QUEUED":
        raise SystemExit("Configure QUEUED execution before enabling deployments")
    sources = [a for s in pipeline["stages"] for a in s["actions"] if a["actionTypeId"]["category"] == "Source"]
    if len(sources) != 1 or sources[0]["configuration"].get("FullRepositoryId") != "05sebas17/campus-orders":
        raise SystemExit("Unexpected source repository")
    if sources[0]["configuration"].get("BranchName") != "main":
        raise SystemExit("Expected source branch main")
    instances = [i for r in ec2.describe_instances(Filters=[
        {"Name": "tag:Name", "Values": ["campus-orders"]},
        {"Name": "instance-state-name", "Values": ["running"]},
    ])["Reservations"] for i in r["Instances"]]
    if len(instances) != 1:
        raise SystemExit("Expected exactly one running campus-orders instance")
    instance = instances[0]
    artifact_store = pipeline.get("artifactStore", {})
    if artifact_store.get("type") != "S3" or artifact_store.get("encryptionKey"):
        raise SystemExit("Expected a single S3 artifact store using default encryption")
    bucket = artifact_store["location"]
    print(json.dumps({"account": ACCOUNT, "region": REGION, "pipeline": PIPELINE,
                      "instance": instance["InstanceId"], "bucket": bucket,
                      "new_stack": STACK, "mode": args.mode}, indent=2))
    outputs = {}
    try:
        stack = cf.describe_stacks(StackName=STACK)["Stacks"][0]
        outputs = {o["OutputKey"]: o["OutputValue"] for o in stack.get("Outputs", [])}
    except cf.exceptions.ClientError as exc:
        if "does not exist" not in str(exc):
            raise
    profile = instance.get("IamInstanceProfile", {}).get("Arn", "").split("/")[-1]
    if profile and profile != outputs.get("InstanceProfileName"):
        raise SystemExit("Instance already has another IAM profile; merge permissions manually, do not replace it")
    if args.mode == "plan":
        cf.validate_template(TemplateBody=(ROOT / "infra/ec2-deploy.yml").read_text())
        session.client("ssm").describe_instance_information(MaxResults=5)
        print("Plan validated. Existing EC2, disk, security group and database will be retained.")
        return
    if args.mode == "prepare":
        subprocess.run([
            "aws", "cloudformation", "deploy", "--region", REGION,
            "--stack-name", STACK, "--template-file", str(ROOT / "infra/ec2-deploy.yml"),
            "--capabilities", "CAPABILITY_IAM", "--no-fail-on-empty-changeset",
            "--parameter-overrides", f"ArtifactBucket={bucket}", f"PipelineName={PIPELINE}",
            "InstanceId=" + instance["InstanceId"],
            "PipelineRoleName=" + pipeline["roleArn"].split("/")[-1],
        ], check=True)
        stack = cf.describe_stacks(StackName=STACK)["Stacks"][0]
        outputs = {o["OutputKey"]: o["OutputValue"] for o in stack["Outputs"]}
        # Recheck immediately before associating; never replace an existing profile.
        current = ec2.describe_instances(InstanceIds=[instance["InstanceId"]])["Reservations"][0]["Instances"][0]
        current_profile = current.get("IamInstanceProfile", {}).get("Arn", "").split("/")[-1]
        if current_profile and current_profile != outputs["InstanceProfileName"]:
            raise SystemExit("Instance profile changed; stopped without replacing it")
        if not current_profile:
            for attempt in range(12):
                try:
                    ec2.associate_iam_instance_profile(InstanceId=instance["InstanceId"],
                        IamInstanceProfile={"Name": outputs["InstanceProfileName"]})
                    break
                except ec2.exceptions.ClientError as exc:
                    if "InvalidParameterValue" not in str(exc) or attempt == 11:
                        raise
                    time.sleep(5)
        ec2.create_tags(Resources=[instance["InstanceId"]], Tags=[{"Key": "CampusOrdersDeploy", "Value": STACK}])
        print("Deployment infrastructure ready. Start the SSM agent on EC2, wait for Online, then run enable.")
        return
    if not outputs or not profile:
        raise SystemExit("Run prepare and start the SSM agent before enable")
    nodes = session.client("ssm").describe_instance_information(Filters=[
        {"Key": "InstanceIds", "Values": [instance["InstanceId"]]}
    ])["InstanceInformationList"]
    if len(nodes) != 1 or nodes[0].get("PingStatus") != "Online":
        raise SystemExit("The target instance must be Online in Systems Manager before enable")
    running = cp.list_pipeline_executions(pipelineName=PIPELINE, maxResults=20)["pipelineExecutionSummaries"]
    if any(e["status"] in ["InProgress", "Stopping"] for e in running):
        raise SystemExit("Wait for the current pipeline execution to finish before enabling Deploy")
    backup = Path.home() / (PIPELINE + "-before-deploy.json")
    if not backup.exists():
        backup.write_text(json.dumps({"pipeline": pipeline}, indent=2))
    cp.update_pipeline(pipeline=updated)
    print("Deploy stage enabled. Push a new commit to main to test the complete automatic flow.")
    print("Previous pipeline configuration saved to " + str(backup))


if __name__ == "__main__":
    main()
