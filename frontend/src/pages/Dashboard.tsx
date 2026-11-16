import { useQuery } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { ArrowRight, Plus } from "lucide-react";
import { api } from "../services/api";
import {
  labels,
  statuses,
  type OrderSummary,
  type Page,
} from "../features/orders/model";
import { ErrorState, Loading, OrdersTable } from "../components/Common";
export default function Dashboard() {
  const stats = useQuery({
    queryKey: ["stats"],
    queryFn: () => api<Record<string, number>>("/orders/stats"),
  });
  const recent = useQuery({
    queryKey: ["orders", "recent"],
    queryFn: () => api<Page<OrderSummary>>("/orders?size=5"),
  });
  return (
    <>
      <header className="page-header">
        <div>
          <div className="eyebrow">VISIÓN GENERAL</div>
          <h1>Resumen de pedidos</h1>
          <p>Consulta la actividad y el estado de tus pedidos.</p>
        </div>
        <Link className="button primary" to="/orders/new">
          <Plus size={18} />
          Nuevo pedido
        </Link>
      </header>
      {stats.isPending ? (
        <Loading />
      ) : stats.isError ? (
        <ErrorState error={stats.error} retry={() => void stats.refetch()} />
      ) : (
        <section className="stats-grid" aria-label="Estadísticas globales">
          <article className="stat total">
            <span>Total de pedidos</span>
            <strong>{stats.data.total}</strong>
            <small>Todos los registros</small>
          </article>
          {statuses.map((status) => (
            <article className="stat" key={status}>
              <span>{labels[status]}</span>
              <strong>{stats.data[status]}</strong>
              <small>
                <i className={`dot dot-${status}`} />
                {status === "DELIVERED"
                  ? "Ciclo completado"
                  : "Seguimiento activo"}
              </small>
            </article>
          ))}
        </section>
      )}
      <section className="panel">
        <div className="panel-heading">
          <div>
            <h2>Pedidos recientes</h2>
            <p>Las últimas solicitudes registradas.</p>
          </div>
          <Link className="text-link" to="/orders">
            Ver todos <ArrowRight size={16} />
          </Link>
        </div>
        {recent.isPending ? (
          <Loading />
        ) : recent.isError ? (
          <ErrorState
            error={recent.error}
            retry={() => void recent.refetch()}
          />
        ) : (
          <OrdersTable orders={recent.data.content} />
        )}
      </section>
      <p className="footnote">
        Datos reales de la base local · Los importes se muestran en pesos
        colombianos (COP).
      </p>
    </>
  );
}
