// Re-export of sonner's toast API as the platform's single notification entrypoint.
// Domain code should import `toast` from here (not from "sonner" directly) so the
// notification provider can be swapped/extended in core/notifications without
// touching every call site.
export { toast } from "sonner";
