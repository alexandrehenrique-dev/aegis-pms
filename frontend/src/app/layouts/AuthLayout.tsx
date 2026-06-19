import { Suspense } from "react";
import { Outlet } from "react-router";

/**
 * Layout wrapper for the public/unauthenticated routes (login, password
 * recovery, invite, tenant/product selection). Each auth page already
 * renders its own full-screen chrome, so this layout simply hosts the
 * router's <Outlet/> — kept as an explicit layout per the architecture
 * blueprint (app/layouts) rather than inlining auth routes directly.
 */
export function AuthLayout() {
  return <Suspense fallback={null}><Outlet /></Suspense>;
}
