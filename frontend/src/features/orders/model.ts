import { z } from "zod";
export const statuses = [
  "PENDING",
  "IN_PROGRESS",
  "SHIPPED",
  "DELIVERED",
] as const;
export type Status = (typeof statuses)[number];
export const labels: Record<Status, string> = {
  PENDING: "Pendiente",
  IN_PROGRESS: "En proceso",
  SHIPPED: "Enviado",
  DELIVERED: "Entregado",
};
export interface Product {
  id: string;
  sku: string;
  name: string;
  description?: string;
  price: number;
  active: boolean;
}
export interface Page<T> {
  content: T[];
  number: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
export interface OrderSummary {
  id: string;
  number: string;
  customerName: string;
  status: Status;
  total: number;
  createdAt: string;
}
export interface Order extends OrderSummary {
  email: string;
  phone?: string;
  address: {
    street: string;
    city: string;
    region: string;
    postalCode?: string;
    country: string;
  };
  currency: string;
  version: number;
  updatedAt: string;
  items: {
    productId: string;
    sku: string;
    name: string;
    quantity: number;
    unitPrice: number;
    subtotal: number;
  }[];
  history: {
    previousStatus: Status | null;
    newStatus: Status;
    changedAt: string;
  }[];
}
const required = (max: number) =>
  z
    .string()
    .trim()
    .min(1, "Este campo es obligatorio.")
    .max(max, `Máximo ${max} caracteres.`);
export const orderSchema = z.object({
  customerName: required(120),
  email: required(254).pipe(z.email("Ingresa un correo válido.")),
  phone: z.string().max(30, "Máximo 30 caracteres."),
  address: z.object({
    street: required(200),
    city: required(100),
    region: required(100),
    postalCode: z.string().max(20),
    country: z
      .string()
      .length(2, "Usa el código de dos letras, por ejemplo CO."),
  }),
  items: z
    .array(
      z.object({
        productId: z.string().uuid("Selecciona un producto."),
        quantity: z
          .number({ error: "Ingresa una cantidad." })
          .int("Usa un número entero.")
          .min(1, "Mínimo 1 unidad.")
          .max(1000, "Máximo 1000 unidades."),
      }),
    )
    .min(1, "Añade al menos un producto.")
    .max(50, "Máximo 50 productos.")
    .refine(
      (items) => new Set(items.map((i) => i.productId)).size === items.length,
      "No repitas productos: modifica su cantidad.",
    ),
});
export type OrderInput = z.infer<typeof orderSchema>;
