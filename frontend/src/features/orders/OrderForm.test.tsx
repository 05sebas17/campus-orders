import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, it, expect, vi } from "vitest";
import { OrderForm } from "./OrderForm";
import { orderSchema } from "./model";
const products = [
  {
    id: "11111111-1111-4111-8111-111111111111",
    sku: "A",
    name: "Cuaderno",
    price: 1000,
    active: true,
  },
];
describe("Crear pedido", () => {
  it("muestra validaciones y evita enviar un formulario vacío", async () => {
    const submit = vi.fn();
    render(<OrderForm products={products} pending={false} onSubmit={submit} />);
    await userEvent.click(screen.getByRole("button", { name: "Crear pedido" }));
    expect(
      await screen.findAllByText("Este campo es obligatorio."),
    ).not.toHaveLength(0);
    expect(submit).not.toHaveBeenCalled();
  });
  it("permite añadir y quitar líneas de productos", async () => {
    render(
      <OrderForm products={products} pending={false} onSubmit={vi.fn()} />,
    );
    await userEvent.click(
      screen.getByRole("button", { name: "Añadir producto" }),
    );
    expect(screen.getAllByRole("combobox")).toHaveLength(2);
    await userEvent.click(
      screen.getByRole("button", { name: "Quitar producto 2" }),
    );
    expect(screen.getAllByRole("combobox")).toHaveLength(1);
  });
  it("rechaza cantidades fraccionarias, productos repetidos y correo inválido", () => {
    const input = {
      customerName: "Cliente",
      email: "a@example.com",
      phone: "",
      address: {
        street: "Calle 1",
        city: "Bogotá",
        region: "Bogotá",
        postalCode: "",
        country: "CO",
      },
      items: [{ productId: products[0].id, quantity: 1 }],
    };
    expect(orderSchema.safeParse(input).success).toBe(true);
    expect(orderSchema.safeParse({ ...input, email: "bad" }).success).toBe(
      false,
    );
    expect(
      orderSchema.safeParse({
        ...input,
        items: [...input.items, ...input.items],
      }).success,
    ).toBe(false);
    expect(
      orderSchema.safeParse({
        ...input,
        items: [{ productId: products[0].id, quantity: 1.5 }],
      }).success,
    ).toBe(false);
  });
  it("bloquea el botón durante el envío", () => {
    render(<OrderForm products={products} pending onSubmit={vi.fn()} />);
    expect(
      screen.getByRole("button", { name: "Creando pedido…" }),
    ).toBeDisabled();
  });
});
