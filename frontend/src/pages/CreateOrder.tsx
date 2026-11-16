import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useNavigate, Link } from "react-router-dom";
import { api } from "../services/api";
import type {
  Product,
  Page,
  Order,
  OrderInput,
} from "../features/orders/model";
import { OrderForm } from "../features/orders/OrderForm";
import { Loading, ErrorState } from "../components/Common";
async function catalog() {
  const products: Product[] = [];
  let page = 0;
  while (true) {
    const result = await api<Page<Product>>(`/products?size=100&page=${page}`);
    products.push(...result.content);
    page++;
    if (page >= result.totalPages) return products;
  }
}
export default function CreateOrder() {
  const client = useQueryClient();
  const navigate = useNavigate();
  const products = useQuery({ queryKey: ["products"], queryFn: catalog });
  const create = useMutation({
    mutationFn: (data: OrderInput) =>
      api<Order>("/orders", { method: "POST", body: JSON.stringify(data) }),
    onSuccess: async (order) => {
      await Promise.all([
        client.invalidateQueries({ queryKey: ["orders"] }),
        client.invalidateQueries({ queryKey: ["stats"] }),
      ]);
      navigate(`/orders/${order.id}`, { state: { created: true } });
    },
  });
  return (
    <>
      <Link className="back-link" to="/orders">
        ← Volver a pedidos
      </Link>
      <header className="page-header">
        <div>
          <div className="eyebrow">UN NUEVO COMIENZO</div>
          <h1>Crear pedido</h1>
          <p>Completa los datos y nosotros hacemos las cuentas.</p>
        </div>
      </header>
      {create.isError && <ErrorState error={create.error} />}
      {products.isPending ? (
        <Loading />
      ) : products.isError ? (
        <ErrorState
          error={products.error}
          retry={() => void products.refetch()}
        />
      ) : !products.data.length ? (
        <div className="panel state">
          No hay productos activos. Carga el catálogo local antes de crear
          pedidos.
        </div>
      ) : (
        <OrderForm
          products={products.data}
          pending={create.isPending}
          onSubmit={async (data) => {
            await create.mutateAsync(data).catch(() => undefined);
          }}
        />
      )}
    </>
  );
}
