import importlib.util
import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location("configure_cd", ROOT / "scripts/configure-cd.py")
configure_cd = importlib.util.module_from_spec(spec)
spec.loader.exec_module(configure_cd)


class DeploymentTests(unittest.TestCase):
    def pipeline(self):
        return {
            "name": "campus-orders-ci", "version": 3, "executionMode": "QUEUED",
            "triggers": [{"providerType": "CodeStarSourceConnection", "gitConfiguration": {
                "sourceActionName": "Source", "push": [{"branches": {"includes": ["main"]}}]}}],
            "stages": [
                {"name": "Source", "actions": [{"name": "Source", "actionTypeId": {
                    "provider": "CodeStarSourceConnection"}, "configuration": {"BranchName": "main"}}]},
                {"name": "Build", "actions": [{"name": "Build", "actionTypeId": {"provider": "CodeBuild"},
                    "outputArtifacts": [{"name": "ActualBuildOutput"}]}]},
            ],
        }

    def test_preserves_source_build_and_trigger_and_uses_actual_artifact(self):
        original = self.pipeline()
        result = configure_cd.add_deploy_stage(original, "app", "group")
        self.assertEqual(result["stages"][:2], original["stages"])
        self.assertEqual(len(original["stages"]), 2)
        self.assertEqual(result["triggers"], original["triggers"])
        self.assertEqual(result["stages"][2]["actions"][0]["inputArtifacts"], [{"name": "ActualBuildOutput"}])

    def test_repeated_enable_is_idempotent(self):
        result = configure_cd.add_deploy_stage(self.pipeline(), "app", "group")
        self.assertEqual(result, configure_cd.add_deploy_stage(result, "app", "group"))

    def test_does_not_replace_different_deployment(self):
        result = configure_cd.add_deploy_stage(self.pipeline(), "app", "group")
        with self.assertRaises(ValueError):
            configure_cd.add_deploy_stage(result, "another-app", "group")

    def test_rejects_ambiguous_build_outputs(self):
        result = self.pipeline()
        result["stages"][1]["actions"][0]["outputArtifacts"].append({"name": "Other"})
        with self.assertRaises(ValueError):
            configure_cd.add_deploy_stage(result, "app", "group")

    def test_revision_matches_built_commit_and_rejects_invalid_value(self):
        with tempfile.TemporaryDirectory() as directory:
            destination = Path(directory) / "frontend/dist"
            destination.mkdir(parents=True)
            env = dict(os.environ, CODEBUILD_RESOLVED_SOURCE_VERSION="a" * 40)
            subprocess.run([sys.executable, str(ROOT / "scripts/write-revision.py")], cwd=directory, env=env, check=True)
            self.assertEqual(json.loads((destination / "version.json").read_text()), {"commit": "a" * 40})
            env["CODEBUILD_RESOLVED_SOURCE_VERSION"] = "not-a-commit"
            process = subprocess.run([sys.executable, str(ROOT / "scripts/write-revision.py")], cwd=directory,
                                     env=env, capture_output=True)
            self.assertNotEqual(process.returncode, 0)
            self.assertEqual(json.loads((destination / "version.json").read_text()), {"commit": "a" * 40})


if __name__ == "__main__":
    unittest.main()
