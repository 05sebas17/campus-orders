import { test, expect } from "@playwright/test";
test("crea, consulta, filtra y avanza un pedido real", async ({ page }) => {
  const name = `Cliente E2E ${Date.now()}`;
  await page.goto("/orders/new");
  await page.getByLabel("Nombre completo").fill(name);
  await page.getByLabel("Correo electrónico").fill("e2e@example.com");
  await page
    .getByLabel("Dirección", { exact: true })
    .fill("Calle de pruebas 42");
  await page.getByLabel("Ciudad", { exact: true }).fill("Bogotá");
  await page.getByLabel("Departamento", { exact: true }).fill("Bogotá D.C.");
  await page
    .getByRole("combobox", { name: "Producto 1", exact: true })
    .selectOption("11111111-1111-4111-8111-111111111111");
  await page.getByLabel("Cantidad", { exact: true }).fill("2");
  await page.getByRole("button", { name: "Crear pedido", exact: true }).click();
  await expect(page.getByRole("status")).toHaveText(
    "Pedido creado correctamente.",
  );
  await expect(page.getByText(name, { exact: true })).toBeVisible();
  for (const next of ["En proceso", "Enviado", "Entregado"]) {
    await page
      .getByRole("button", { name: `Marcar: ${next}`, exact: true })
      .click();
    await expect(page.getByRole("dialog")).toBeVisible();
    await page
      .getByRole("button", { name: "Confirmar cambio", exact: true })
      .click();
    await expect(page.getByRole("dialog")).not.toBeVisible();
  }
  await expect(
    page.getByText("Pedido entregado", { exact: true }),
  ).toBeVisible();
  const detail = page.url();
  await page.reload();
  await expect(page.getByText(name, { exact: true })).toBeVisible();
  await page.goto("/orders");
  await page.getByLabel("Buscar pedido").fill(name);
  await page
    .getByRole("combobox", { name: "Estado", exact: true })
    .selectOption("DELIVERED");
  await page.getByRole("button", { name: "Aplicar filtros" }).click();
  await expect(page.locator("tbody tr")).toHaveCount(1);
  await expect(page.locator("tbody")).toContainText(name);
  await page.goto(detail);
  await expect(
    page.getByText("Pedido entregado", { exact: true }),
  ).toBeVisible();
});
test("la vista móvil no desborda horizontalmente", async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto("/");
  await expect(
    page.getByRole("heading", { name: "Resumen de pedidos" }),
  ).toBeVisible();
  await expect(page.locator("tbody tr").first()).toBeVisible();
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= window.innerWidth,
    ),
  ).toBe(true);
});

