import { BrowserRouter } from "react-router";
import { ThemeProvider } from "../providers/ThemeProvider";
import { AuthProvider } from "../../core/auth/AuthContext";
import { ViewAsRoleProvider } from "../../core/permissions/ViewAsRoleContext";
import { AppRoutes } from "../routes";

/** Composition root: providers + router, mounted once by main.tsx. */
export function Bootstrap() {
  return (
    <BrowserRouter>
      <ThemeProvider>
        <AuthProvider>
          <ViewAsRoleProvider>
            <AppRoutes />
          </ViewAsRoleProvider>
        </AuthProvider>
      </ThemeProvider>
    </BrowserRouter>
  );
}
