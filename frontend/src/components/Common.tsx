import { Link } from "react-router-dom";
import { ArrowUpRight, AlertCircle, PackageOpen } from "lucide-react";
import type { OrderSummary, Status } from "../features/orders/model";
import { labels } from "../features/orders/model";
import { ApiError, date, money } from "../services/api";
export function Badge({ status }: { status: Status }) {
  return (
    <span className={`badge badge-${status}`}>
      <span />
      {labels[status]}
    </span>
  );
}
export function Loading() {
  return (
    <div className="panel state" role="status">
      <span className="spinner" />
      Cargando datos…
    </div>
  );
}
export function ErrorState({
  error,
  retry,
}: {
  error: Error;
  retry?: () => void;
}) {
  return (
    <div className="error-box" role="alert">
      <AlertCircle size={20} />
      <div>
        <strong>{error.message}</strong>
        {error instanceof ApiError && (
          <>
            <ul>
              {Object.entries(error.fields || {}).map(([key, value]) => (
                <li key={key}>
                  {key}: {value}
                </li>
              ))}
            </ul>
            {error.traceId && <small>Referencia: {error.traceId}</small>}
          </>
        )}
        {retry && (
          <button className="button secondary mt-3" onClick={retry}>
            Reintentar
          </button>
        )}
      </div>
    </div>
  );
}
export function OrdersTable({ orders }: { orders: OrderSummary[] }) {
  if (!orders.length)
    return (
      <div className="state">
        <PackageOpen size={32} />
        <h3>No hay pedidos para mostrar</h3>
        <p>Prueba otros filtros o registra tu primer pedido.</p>
        <Link className="button primary" to="/orders/new">
          Crear pedido
        </Link>
      </div>
    );
  return (
    <div className="table-scroll">
      <table>
        <thead>
          <tr>
            <th>Pedido</th>
            <th>Cliente</th>
            <th>Fecha de creación</th>
            <th>Total</th>
            <th>Estado</th>
            <th>
              <span className="sr-only">Acciones</span>
            </th>
          </tr>
        </thead>
        <tbody>
          {orders.map((order) => (
            <tr key={order.id}>
              <td>
                <Link
                  className="order-number"
                  title={order.number}
                  to={`/orders/${order.id}`}
                >
                  {order.number}
                </Link>
              </td>
              <td>{order.customerName}</td>
              <td className="muted nowrap">{date(order.createdAt)}</td>
              <td className="nowrap font-semibold">{money(order.total)}</td>
              <td>
                <Badge status={order.status} />
              </td>
              <td>
                <Link
                  className="icon-button"
                  aria-label={`Ver pedido ${order.number}`}
                  to={`/orders/${order.id}`}
                >
                  <ArrowUpRight size={18} />
                </Link>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
