import React from "react";
import ReactDOM from "react-dom/client";
import {
  BrowserRouter,
  NavLink,
  Routes,
  Route,
  Link,
  useLocation,
} from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import {
  LayoutDashboard,
  Package,
  Plus,
  ArrowUpRight,
} from "lucide-react";
import Dashboard from "./pages/Dashboard";
import OrderList from "./pages/OrderList";
import CreateOrder from "./pages/CreateOrder";
import OrderDetail from "./pages/OrderDetail";
import "./styles.css";
const client = new QueryClient({
  defaultOptions: { queries: { retry: 1, staleTime: 15000 } },
});
function App() {
  const location = useLocation();
  return (
    <div className="app-shell">
      <a href="#main" className="skip-link">
        Saltar al contenido
      </a>
      <aside className="sidebar">
        <Link to="/" className="brand">
          <span className="brand-icon">
            <Package size={25} />
          </span>
          <span>
            campus<span className="brand-caption">GESTIÓN DE PEDIDOS</span>
          </span>
        </Link>
        <div className="nav-label">ESPACIO DE TRABAJO</div>
        <nav aria-label="Navegación principal">
          <NavLink to="/" end>
            <LayoutDashboard size={19} />
            Resumen
          </NavLink>
          <NavLink to="/orders" end>
            <Package size={19} />
            Pedidos
          </NavLink>
          <NavLink to="/orders/new">
            <Plus size={19} />
            Crear pedido
          </NavLink>
        </nav>
        <div className="sidebar-bottom">
          <span className="avatar">U</span>
          <div>
            Campus / Universidad<small>Demostración sin autenticación</small>
          </div>
        </div>
      </aside>
      <div className="main-shell">
        <div className="topbar">
          <div>
            Panel <span>/</span>{" "}
            <strong>{location.pathname === "/" ? "Resumen" : "Pedidos"}</strong>
          </div>
          <a href="/swagger-ui/index.html" target="_blank" rel="noreferrer">
            API & documentación <ArrowUpRight size={14} />
          </a>
        </div>
        <main id="main">
          <Routes>
            <Route path="/" element={<Dashboard />} />
            <Route path="/orders" element={<OrderList />} />
            <Route path="/orders/new" element={<CreateOrder />} />
            <Route path="/orders/:id" element={<OrderDetail />} />
            <Route
              path="*"
              element={
                <div className="state">
                  <h1>Página no encontrada</h1>
                  <Link to="/">Volver al resumen</Link>
                </div>
              }
            />
          </Routes>
        </main>
      </div>
    </div>
  );
}
ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <QueryClientProvider client={client}>
      <BrowserRouter>
        <App />
      </BrowserRouter>
    </QueryClientProvider>
  </React.StrictMode>,
);
