import { useEffect, useRef, useState } from "react";
import { useParams, Link, useLocation } from "react-router-dom";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { ArrowRight, CheckCircle2, MapPin, UserRound } from "lucide-react";
import { api, date, money, ApiError } from "../services/api";
import { statuses, labels, type Order } from "../features/orders/model";
import { Badge, Loading, ErrorState } from "../components/Common";
export default function OrderDetail() {
  const { id } = useParams();
  const location = useLocation();
  const client = useQueryClient();
  const dialog = useRef<HTMLDialogElement>(null);
  const [confirm, setConfirm] = useState(false);
  const query = useQuery({
    queryKey: ["order", id],
    queryFn: () => api<Order>(`/orders/${id}`),
  });
  const change = useMutation({
    mutationFn: () =>
      api<Order>(`/orders/${id}/status`, {
        method: "PATCH",
        body: JSON.stringify({
          status: statuses[statuses.indexOf(query.data!.status) + 1],
          version: query.data!.version,
        }),
      }),
    onSuccess: async (order) => {
      client.setQueryData(["order", id], order);
      setConfirm(false);
      await Promise.all([
        client.invalidateQueries({ queryKey: ["orders"] }),
        client.invalidateQueries({ queryKey: ["stats"] }),
      ]);
    },
    onError: (error) => {
      setConfirm(false);
      if (error instanceof ApiError && error.status === 409)
        void query.refetch();
    },
  });
  useEffect(() => {
    if (confirm) dialog.current?.showModal();
    else dialog.current?.close();
  }, [confirm]);
  if (query.isPending) return <Loading />;
  if (query.isError)
    return (
      <ErrorState error={query.error} retry={() => void query.refetch()} />
    );
  const order = query.data;
  const next = statuses[statuses.indexOf(order.status) + 1];
  return (
    <>
      <Link className="back-link" to="/orders">
        ← Volver a pedidos
      </Link>
      {location.state?.created && (
        <div className="success-box" role="status">
          <CheckCircle2 size={19} />
          Pedido creado correctamente.
        </div>
      )}
      <header className="page-header">
        <div>
          <div className="eyebrow">DETALLE DEL PEDIDO</div>
          <h1 className="detail-number">{order.number}</h1>
          <p>Creado el {date(order.createdAt)}</p>
        </div>
        <Badge status={order.status} />
      </header>
      {change.isError && <ErrorState error={change.error} />}
      <div className="detail-grid">
        <div>
          <section className="panel detail-info">
            <div>
              <h2>
                <UserRound size={18} />
                Cliente
              </h2>
              <strong>{order.customerName}</strong>
              <p>{order.email}</p>
              {order.phone && <p>{order.phone}</p>}
            </div>
            <div>
              <h2>
                <MapPin size={18} />
                Envío
              </h2>
              <strong>{order.address.street}</strong>
              <p>
                {order.address.city}, {order.address.region}
              </p>
              <p>
                {order.address.postalCode} · {order.address.country}
              </p>
            </div>
          </section>
          <section className="panel mt-6">
            <div className="panel-heading">
              <h2>Productos del pedido</h2>
              <span className="muted">Precios al comprar</span>
            </div>
            <div className="table-scroll">
              <table>
                <thead>
                  <tr>
                    <th>Producto</th>
                    <th>Precio unitario</th>
                    <th>Cant.</th>
                    <th>Subtotal</th>
                  </tr>
                </thead>
                <tbody>
                  {order.items.map((item) => (
                    <tr key={item.productId}>
                      <td>
                        <strong>{item.name}</strong>
                        <small className="block muted">{item.sku}</small>
                      </td>
                      <td>{money(item.unitPrice)}</td>
                      <td>{item.quantity}</td>
                      <td>{money(item.subtotal)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <div className="detail-total">
              <span>
                Total del pedido <small>COP</small>
              </span>
              <strong>{money(order.total)}</strong>
            </div>
          </section>
        </div>
        <aside className="panel timeline-panel">
          <div className="eyebrow">PASO A PASO</div>
          <h2>Seguimiento</h2>
          <ol className="timeline">
            {statuses.map((status) => {
              const event = order.history.find((h) => h.newStatus === status);
              return (
                <li key={status} className={event ? "done" : ""}>
                  <span className="timeline-dot">{event ? "✓" : "·"}</span>
                  <strong>{labels[status]}</strong>
                  <small>
                    {event ? date(event.changedAt) : "Por completar"}
                  </small>
                </li>
              );
            })}
          </ol>
          {next ? (
            <button
              className="button primary w-full"
              disabled={change.isPending}
              onClick={() => setConfirm(true)}
            >
              Marcar: {labels[next]}
              <ArrowRight size={17} />
            </button>
          ) : (
            <div className="success-box">
              <CheckCircle2 size={20} />
              Pedido entregado
            </div>
          )}
          <p className="footnote">
            Última actualización: {date(order.updatedAt)}
          </p>
        </aside>
      </div>
      <dialog
        ref={dialog}
        onCancel={() => setConfirm(false)}
        aria-labelledby="confirm-title"
      >
        <h2 id="confirm-title">Confirmar cambio de estado</h2>
        <p>
          El pedido avanzará a <strong>{next && labels[next]}</strong>. Este
          cambio no se puede revertir.
        </p>
        <div className="dialog-actions">
          <button
            className="button secondary"
            disabled={change.isPending}
            onClick={() => setConfirm(false)}
          >
            Cancelar
          </button>
          <button
            className="button primary"
            disabled={change.isPending}
            onClick={() => change.mutate()}
          >
            {change.isPending ? "Actualizando…" : "Confirmar cambio"}
          </button>
        </div>
      </dialog>
    </>
  );
}
