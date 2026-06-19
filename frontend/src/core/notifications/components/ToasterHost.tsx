import { Toaster } from "sonner";

/** Single mount point for the toast/notification stack, used once in AppShell. */
export function ToasterHost() {
  return <Toaster richColors position="bottom-right" />;
}
