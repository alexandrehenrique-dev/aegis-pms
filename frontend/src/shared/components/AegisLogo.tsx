import logoUrl from "../assets/logo.png";

export function AegisLogo({ size = "md", className = "" }: { size?: "xs" | "sm" | "md" | "lg"; className?: string }) {
  const h = { xs: "h-5 w-5", sm: "h-7 w-7", md: "h-10 w-10", lg: "h-14 w-14" }[size];
  return <img src={logoUrl} alt="Aegis PMS" className={`${h} object-contain ${className}`} />;
}
