import { render, screen } from "@testing-library/react";
import { it, expect } from "vitest";
import { ErrorState } from "./Common";
import { ApiError } from "../services/api";
it("presenta un error del servidor con sus campos y referencia", () => {
  render(
    <ErrorState
      error={
        new ApiError(
          "Datos inválidos",
          400,
          { email: "Correo inválido" },
          "trace-123",
        )
      }
    />,
  );
  expect(screen.getByRole("alert")).toHaveTextContent("Datos inválidos");
  expect(screen.getByText("email: Correo inválido")).toBeInTheDocument();
  expect(screen.getByText("Referencia: trace-123")).toBeInTheDocument();
});
