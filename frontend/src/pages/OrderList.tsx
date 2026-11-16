import { useState } from "react";
import { useQuery, keepPreviousData } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { Plus, Search, ChevronLeft, ChevronRight } from "lucide-react";
import { api } from "../services/api";
import {
  labels,
  statuses,
  type OrderSummary,
  type Page,
} from "../features/orders/model";
import { ErrorState, Loading, OrdersTable } from "../components/Common";
export default function OrderList() {
  const [page, setPage] = useState(0);
  const [filters, setFilters] = useState({
    search: "",
    status: "",
    from: "",
    to: "",
    sort: "createdAt",
    direction: "desc",
  });
  const params = new URLSearchParams({
    page: String(page),
    size: "10",
    sort: filters.sort,
    direction: filters.direction,
  });
  if (filters.search) params.set("search", filters.search);
  if (filters.status) params.set("status", filters.status);
  if (filters.from)
    params.set("from", new Date(`${filters.from}T00:00:00`).toISOString());
  if (filters.to) {
    const end = new Date(`${filters.to}T00:00:00`);
    end.setDate(end.getDate() + 1);
    params.set("to", end.toISOString());
  }
  const query = useQuery({
    queryKey: ["orders", params.toString()],
    queryFn: () => api<Page<OrderSummary>>(`/orders?${params}`),
    placeholderData: keepPreviousData,
  });
  return (
    <>
      <header className="page-header">
        <div>
          <div className="eyebrow">OPERACIÓN</div>
          <h1>Todos los pedidos</h1>
          <p>Busca, organiza y sigue cada solicitud.</p>
        </div>
        <Link className="button primary" to="/orders/new">
          <Plus size={18} />
          Nuevo pedido
        </Link>
      </header>
      <section className="panel">
        <form
          className="filters"
          onSubmit={(event) => {
            event.preventDefault();
            const data = new FormData(event.currentTarget);
            setFilters(Object.fromEntries(data) as typeof filters);
            setPage(0);
          }}
        >
          <label className="search-field">
            Buscar pedido
            <div className="input-icon">
              <Search size={18} />
              <input
                name="search"
                maxLength={120}
                placeholder="Número, cliente o correo"
              />
            </div>
          </label>
          <label>
            Estado
            <select name="status">
              <option value="">Todos los estados</option>
              {statuses.map((status) => (
                <option value={status} key={status}>
                  {labels[status]}
                </option>
              ))}
            </select>
          </label>
          <label>
            Desde
            <input type="date" name="from" />
          </label>
          <label>
            Hasta (inclusive)
            <input type="date" name="to" />
          </label>
          <label>
            Ordenar por
            <select name="sort">
              <option value="createdAt">Fecha</option>
              <option value="total">Total</option>
              <option value="status">Estado</option>
            </select>
          </label>
          <label>
            Dirección
            <select name="direction">
              <option value="desc">Descendente</option>
              <option value="asc">Ascendente</option>
            </select>
          </label>
          <button className="button primary" type="submit">
            Aplicar filtros
          </button>
          <button
            type="reset"
            className="button secondary"
            onClick={() => {
              setPage(0);
              setFilters({
                search: "",
                status: "",
                from: "",
                to: "",
                sort: "createdAt",
                direction: "desc",
              });
            }}
          >
            Limpiar
          </button>
        </form>
        {query.isPending ? (
          <Loading />
        ) : query.isError ? (
          <ErrorState error={query.error} retry={() => void query.refetch()} />
        ) : (
          <>
            <div aria-busy={query.isFetching}>
              <OrdersTable orders={query.data.content} />
            </div>
            <div className="pagination">
              <span>
                {query.data.totalElements} pedidos · Página {page + 1} de{" "}
                {Math.max(1, query.data.totalPages)}
              </span>
              <div>
                <button
                  className="icon-button"
                  aria-label="Página anterior"
                  disabled={page === 0 || query.isFetching}
                  onClick={() => setPage(page - 1)}
                >
                  <ChevronLeft size={18} />
                </button>
                <button
                  className="icon-button"
                  aria-label="Página siguiente"
                  disabled={
                    page + 1 >= query.data.totalPages || query.isFetching
                  }
                  onClick={() => setPage(page + 1)}
                >
                  <ChevronRight size={18} />
                </button>
              </div>
            </div>
          </>
        )}
      </section>
    </>
  );
}
