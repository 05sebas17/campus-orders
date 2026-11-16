import { useRef } from "react";
import { useForm, useFieldArray, useWatch } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Plus, Trash2, ArrowRight } from "lucide-react";
import { orderSchema, type OrderInput, type Product } from "./model";
import { money } from "../../services/api";
export function OrderForm({
  products,
  pending,
  onSubmit,
}: {
  products: Product[];
  pending: boolean;
  onSubmit: (data: OrderInput) => Promise<void>;
}) {
  const lock = useRef(false);
  const {
    register,
    control,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<OrderInput>({
    resolver: zodResolver(orderSchema),
    defaultValues: {
      customerName: "",
      email: "",
      phone: "",
      address: {
        street: "",
        city: "",
        region: "",
        postalCode: "",
        country: "CO",
      },
      items: [{ productId: "", quantity: 1 }],
    },
  });
  const { fields, append, remove } = useFieldArray({ control, name: "items" });
  const items = useWatch({ control, name: "items" });
  const estimated = items.reduce(
    (sum, item) =>
      sum +
      (products.find((p) => p.id === item.productId)?.price || 0) *
        (Number.isFinite(item.quantity) ? item.quantity : 0),
    0,
  );
  const submit = handleSubmit(async (data) => {
    if (lock.current) return;
    lock.current = true;
    try {
      await onSubmit(data);
    } finally {
      lock.current = false;
    }
  });
  return (
    <form onSubmit={submit} noValidate className="create-layout">
      <div className="form-sections">
        <section className="panel form-panel">
          <div className="section-title">
            <span>01</span>
            <div>
              <h2>Datos del cliente</h2>
              <p>La información de contacto para este pedido.</p>
            </div>
          </div>
          <div className="form-grid">
            <label className="full">
              Nombre completo
              <input
                {...register("customerName")}
                autoComplete="name"
                aria-invalid={!!errors.customerName}
              />
              {errors.customerName && (
                <small role="alert">{errors.customerName.message}</small>
              )}
            </label>
            <label>
              Correo electrónico
              <input
                {...register("email")}
                type="email"
                autoComplete="email"
                aria-invalid={!!errors.email}
              />
              {errors.email && (
                <small role="alert">{errors.email.message}</small>
              )}
            </label>
            <label>
              Teléfono <span className="optional">(opcional)</span>
              <input {...register("phone")} type="tel" autoComplete="tel" />
              {errors.phone && (
                <small role="alert">{errors.phone.message}</small>
              )}
            </label>
          </div>
        </section>
        <section className="panel form-panel">
          <div className="section-title">
            <span>02</span>
            <div>
              <h2>Dirección de envío</h2>
              <p>Indica dónde debemos entregar el pedido.</p>
            </div>
          </div>
          <div className="form-grid">
            <label className="full">
              Dirección
              <input
                {...register("address.street")}
                autoComplete="street-address"
              />
              {errors.address?.street && (
                <small role="alert">{errors.address.street.message}</small>
              )}
            </label>
            <label>
              Ciudad
              <input
                {...register("address.city")}
                autoComplete="address-level2"
              />
              {errors.address?.city && (
                <small role="alert">{errors.address.city.message}</small>
              )}
            </label>
            <label>
              Departamento
              <input
                {...register("address.region")}
                autoComplete="address-level1"
              />
              {errors.address?.region && (
                <small role="alert">{errors.address.region.message}</small>
              )}
            </label>
            <label>
              Código postal <span className="optional">(opcional)</span>
              <input
                {...register("address.postalCode")}
                autoComplete="postal-code"
              />
              {errors.address?.postalCode && (
                <small role="alert">{errors.address.postalCode.message}</small>
              )}
            </label>
            <label>
              País (código)
              <input
                {...register("address.country")}
                maxLength={2}
                autoComplete="country"
              />
              {errors.address?.country && (
                <small role="alert">{errors.address.country.message}</small>
              )}
            </label>
          </div>
        </section>
        <section className="panel form-panel">
          <div className="section-title">
            <span>03</span>
            <div>
              <h2>Productos</h2>
              <p>Selecciona los productos y sus cantidades.</p>
            </div>
          </div>
          {fields.map((field, index) => (
            <div key={field.id} className="product-row">
              <label>
                Producto {index + 1}
                <select {...register(`items.${index}.productId`)}>
                  <option value="">Seleccionar producto</option>
                  {products.map((p) => (
                    <option key={p.id} value={p.id}>
                      {p.name} · {money(p.price)}
                    </option>
                  ))}
                </select>
                {errors.items?.[index]?.productId && (
                  <small role="alert">
                    {errors.items[index]?.productId?.message}
                  </small>
                )}
              </label>
              <label>
                Cantidad
                <input
                  type="number"
                  min={1}
                  max={1000}
                  {...register(`items.${index}.quantity`, {
                    valueAsNumber: true,
                  })}
                />
                {errors.items?.[index]?.quantity && (
                  <small role="alert">
                    {errors.items[index]?.quantity?.message}
                  </small>
                )}
              </label>
              <button
                type="button"
                className="icon-button delete"
                aria-label={`Quitar producto ${index + 1}`}
                onClick={() => remove(index)}
              >
                <Trash2 size={18} />
              </button>
            </div>
          ))}
          {errors.items && (
            <p role="alert" className="field-error">
              {errors.items.root?.message || errors.items.message}
            </p>
          )}
          <button
            type="button"
            className="button secondary mt-4"
            disabled={fields.length >= 50}
            onClick={() => append({ productId: "", quantity: 1 })}
          >
            <Plus size={16} />
            Añadir producto
          </button>
        </section>
      </div>
      <aside className="panel summary">
        <div className="eyebrow">ANTES DE CONFIRMAR</div>
        <h2>Resumen del pedido</h2>
        <div className="summary-lines">
          {items.map((item, index) => {
            const product = products.find((p) => p.id === item.productId);
            return product ? (
              <div key={index}>
                <span>
                  {product.name}
                  <small>
                    {item.quantity || 0} × {money(product.price)}
                  </small>
                </span>
                <strong>{money(product.price * (item.quantity || 0))}</strong>
              </div>
            ) : null;
          })}
        </div>
        <div className="summary-total">
          <span>
            Total estimado <small>COP</small>
          </span>
          <strong>{money(estimated)}</strong>
        </div>
        <p>El precio final se calcula en el servidor al crear el pedido.</p>
        <button
          type="submit"
          className="button primary w-full"
          disabled={pending || isSubmitting || !products.length}
        >
          {pending || isSubmitting ? "Creando pedido…" : "Crear pedido"}
          <ArrowRight size={18} />
        </button>
        <small>El pedido se registrará como pendiente.</small>
      </aside>
    </form>
  );
}
