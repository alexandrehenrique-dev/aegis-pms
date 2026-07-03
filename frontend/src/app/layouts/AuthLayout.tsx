import { Suspense } from "react";
import { Outlet } from "react-router";
import { ToasterHost } from "../../core/notifications/components/ToasterHost";

/**
 * Layout wrapper for the public/unauthenticated routes (login, password
 * recovery, invite, tenant/product selection). Each auth page already
 * renders its own full-screen chrome, so this layout simply hosts the
 * router's <Outlet/> — kept as an explicit layout per the architecture
 * blueprint (app/layouts) rather than inlining auth routes directly.
 *
 * `ToasterHost` is mounted here (not just in `AppShell`) since Sprint 21
 * added a post-activação/reset toast shown on `LoginScreen`, which lives
 * under this layout, before the user ever reaches `AppShell`.
 */
export function AuthLayout() {
  return <Suspense fallback={null}><Outlet /><ToasterHost /></Suspense>;
}
