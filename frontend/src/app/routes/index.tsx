import { lazy } from "react";
import { Navigate, Route, Routes } from "react-router";
import { AppShell } from "../layouts/AppShell";
import { AuthLayout } from "../layouts/AuthLayout";
import { RequireAuth } from "../guards/RequireAuth";
import { RequireRole } from "../guards/RequireRole";

// --- core/auth (public routes) ---
const LoginScreen = lazy(() => import("../../core/auth/pages/LoginScreen").then((m) => ({ default: m.LoginScreen })));
const ForgotPasswordScreen = lazy(() => import("../../core/auth/pages/ForgotPasswordScreen").then((m) => ({ default: m.ForgotPasswordScreen })));
const ForgotPasswordSentScreen = lazy(() => import("../../core/auth/pages/ForgotPasswordSentScreen").then((m) => ({ default: m.ForgotPasswordSentScreen })));
const ResetPasswordScreen = lazy(() => import("../../core/auth/pages/ResetPasswordScreen").then((m) => ({ default: m.ResetPasswordScreen })));
const InviteScreen = lazy(() => import("../../core/auth/pages/InviteScreen").then((m) => ({ default: m.InviteScreen })));
const TenantSelectScreen = lazy(() => import("../../core/auth/pages/TenantSelectScreen").then((m) => ({ default: m.TenantSelectScreen })));
const ProductSelectScreen = lazy(() => import("../../core/auth/pages/ProductSelectScreen").then((m) => ({ default: m.ProductSelectScreen })));

// --- domains/dashboard ---
const DashboardGlobal = lazy(() => import("../../domains/dashboard/pages/DashboardGlobal").then((m) => ({ default: m.DashboardGlobal })));

// --- domains/tenants (Super Admin) — tela única; criar/editar/excluir são modais dentro dela, não rotas. ---
const TenantsManagement = lazy(() => import("../../domains/tenants/pages/TenantsManagement").then((m) => ({ default: m.TenantsManagement })));

// --- domains/products ---
const ProductsList = lazy(() => import("../../domains/products/pages/ProductsList").then((m) => ({ default: m.ProductsList })));
const CreateProductForm = lazy(() => import("../../domains/products/pages/CreateProductForm").then((m) => ({ default: m.CreateProductForm })));
const ProductDashboard = lazy(() => import("../../domains/products/pages/ProductDashboard").then((m) => ({ default: m.ProductDashboard })));
const ProductDetail = lazy(() => import("../../domains/products/pages/ProductDetail").then((m) => ({ default: m.ProductDetail })));
const ModulesPage = lazy(() => import("../../domains/products/pages/ModulesPage").then((m) => ({ default: m.ModulesPage })));

// --- domains/content ---
const EditorialDashboard = lazy(() => import("../../domains/content/pages/EditorialDashboard").then((m) => ({ default: m.EditorialDashboard })));
const ContentDataGrid = lazy(() => import("../../domains/content/pages/ContentDataGrid").then((m) => ({ default: m.ContentDataGrid })));
const WorkflowBoard = lazy(() => import("../../domains/content/pages/WorkflowBoard").then((m) => ({ default: m.WorkflowBoard })));
const ContentEditor = lazy(() => import("../../domains/content/pages/ContentEditor").then((m) => ({ default: m.ContentEditor })));
const ResponsivePreviewFrame = lazy(() => import("../../domains/content/pages/ResponsivePreviewFrame").then((m) => ({ default: m.ResponsivePreviewFrame })));
const PublishPanel = lazy(() => import("../../domains/content/pages/PublishPanel").then((m) => ({ default: m.PublishPanel })));
const VersionsPage = lazy(() => import("../../domains/content/pages/VersionsPage").then((m) => ({ default: m.VersionsPage })));
const VersionCompareView = lazy(() => import("../../domains/content/pages/VersionCompareView").then((m) => ({ default: m.VersionCompareView })));

// --- domains/assets ---
const AssetLibrary = lazy(() => import("../../domains/assets/pages/AssetLibrary").then((m) => ({ default: m.AssetLibrary })));
const AssetUploadScreen = lazy(() => import("../../domains/assets/pages/AssetUploadScreen").then((m) => ({ default: m.AssetUploadScreen })));
const AssetDetail = lazy(() => import("../../domains/assets/pages/AssetDetail").then((m) => ({ default: m.AssetDetail })));
const AssetMetadataForm = lazy(() => import("../../domains/assets/pages/AssetMetadataForm").then((m) => ({ default: m.AssetMetadataForm })));
const AssetTagManager = lazy(() => import("../../domains/assets/pages/AssetTagManager").then((m) => ({ default: m.AssetTagManager })));
const AssetUsageScreen = lazy(() => import("../../domains/assets/pages/AssetUsageScreen").then((m) => ({ default: m.AssetUsageScreen })));
const AssetPicker = lazy(() => import("../../domains/assets/pages/AssetPicker").then((m) => ({ default: m.AssetPicker })));

// --- domains/forms ---
const FormsDashboard = lazy(() => import("../../domains/forms/pages/FormsDashboard").then((m) => ({ default: m.FormsDashboard })));
const FormsList = lazy(() => import("../../domains/forms/pages/FormsList").then((m) => ({ default: m.FormsList })));
const FormBuilder = lazy(() => import("../../domains/forms/pages/FormBuilder").then((m) => ({ default: m.FormBuilder })));
const FormPreviewFrame = lazy(() => import("../../domains/forms/pages/FormPreviewFrame").then((m) => ({ default: m.FormPreviewFrame })));
const SubmissionTable = lazy(() => import("../../domains/forms/pages/SubmissionTable").then((m) => ({ default: m.SubmissionTable })));
const SubmissionDetails = lazy(() => import("../../domains/forms/pages/SubmissionDetails").then((m) => ({ default: m.SubmissionDetails })));
const BasicFormAnalytics = lazy(() => import("../../domains/forms/pages/BasicFormAnalytics").then((m) => ({ default: m.BasicFormAnalytics })));
const PublicationPanel = lazy(() => import("../../domains/forms/pages/PublicationPanel").then((m) => ({ default: m.PublicationPanel })));

// --- domains/analytics ---
const AnalyticsOverview = lazy(() => import("../../domains/analytics/pages/AnalyticsOverview").then((m) => ({ default: m.AnalyticsOverview })));
const ProductHealthPanel = lazy(() => import("../../domains/analytics/pages/ProductHealthPanel").then((m) => ({ default: m.ProductHealthPanel })));
const ContentAnalytics = lazy(() => import("../../domains/analytics/pages/ContentAnalytics").then((m) => ({ default: m.ContentAnalytics })));
const FormAnalyticsModule = lazy(() => import("../../domains/analytics/pages/FormAnalyticsModule").then((m) => ({ default: m.FormAnalyticsModule })));
const ChannelBreakdown = lazy(() => import("../../domains/analytics/pages/ChannelBreakdown").then((m) => ({ default: m.ChannelBreakdown })));
const ReportGrid = lazy(() => import("../../domains/analytics/pages/ReportGrid").then((m) => ({ default: m.ReportGrid })));
const TrendCards = lazy(() => import("../../domains/analytics/pages/TrendCards").then((m) => ({ default: m.TrendCards })));
const AnalyticsStates = lazy(() => import("../../domains/analytics/pages/AnalyticsStates").then((m) => ({ default: m.AnalyticsStates })));

// --- domains/knowledge ---
const KnowledgeOverview = lazy(() => import("../../domains/knowledge/pages/KnowledgeOverview").then((m) => ({ default: m.KnowledgeOverview })));
const GraphCanvasView = lazy(() => import("../../domains/knowledge/pages/GraphCanvasView").then((m) => ({ default: m.GraphCanvasView })));
const RelationshipExplorer = lazy(() => import("../../domains/knowledge/pages/RelationshipExplorer").then((m) => ({ default: m.RelationshipExplorer })));
const EntityDetails = lazy(() => import("../../domains/knowledge/pages/EntityDetails").then((m) => ({ default: m.EntityDetails })));
const EntitySearch = lazy(() => import("../../domains/knowledge/pages/EntitySearch").then((m) => ({ default: m.EntitySearch })));
const OrphanEntityTable = lazy(() => import("../../domains/knowledge/pages/OrphanEntityTable").then((m) => ({ default: m.OrphanEntityTable })));
const KnowledgeInsights = lazy(() => import("../../domains/knowledge/pages/KnowledgeInsights").then((m) => ({ default: m.KnowledgeInsights })));

// --- domains/settings ---
const SettingsOverview = lazy(() => import("../../domains/settings/pages/SettingsOverview").then((m) => ({ default: m.SettingsOverview })));
const ProductSettings = lazy(() => import("../../domains/settings/pages/ProductSettings").then((m) => ({ default: m.ProductSettings })));
const TenantSettings = lazy(() => import("../../domains/settings/pages/TenantSettings").then((m) => ({ default: m.TenantSettings })));
const PermissionMatrixView = lazy(() => import("../../domains/settings/pages/PermissionMatrixView").then((m) => ({ default: m.PermissionMatrixView })));
const RoleManagement = lazy(() => import("../../domains/settings/pages/RoleManagement").then((m) => ({ default: m.RoleManagement })));
const AccessPreviewPanel = lazy(() => import("../../domains/settings/pages/AccessPreviewPanel").then((m) => ({ default: m.AccessPreviewPanel })));
const SecuritySettingsPanel = lazy(() => import("../../domains/settings/pages/SecuritySettingsPanel").then((m) => ({ default: m.SecuritySettingsPanel })));

// --- domains/users ---
const UserTable = lazy(() => import("../../domains/users/pages/UserTable").then((m) => ({ default: m.UserTable })));
const InviteUserDrawer = lazy(() => import("../../domains/users/pages/InviteUserDrawer").then((m) => ({ default: m.InviteUserDrawer })));
const UserDetailPanel = lazy(() => import("../../domains/users/pages/UserDetailPanel").then((m) => ({ default: m.UserDetailPanel })));

// --- domains/audit ---
const AuditTimeline = lazy(() => import("../../domains/audit/pages/AuditTimeline").then((m) => ({ default: m.AuditTimeline })));
const AuditEventDetail = lazy(() => import("../../domains/audit/pages/AuditEventDetail").then((m) => ({ default: m.AuditEventDetail })));

export function AppRoutes() {
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/dashboard" replace />} />

      <Route element={<AuthLayout />}>
        <Route path="/login" element={<LoginScreen />} />
        <Route path="/forgot-password" element={<ForgotPasswordScreen />} />
        <Route path="/forgot-password/sent" element={<ForgotPasswordSentScreen />} />
        <Route path="/reset-password" element={<ResetPasswordScreen />} />
        <Route path="/invite" element={<InviteScreen />} />
        <Route path="/select-tenant" element={<TenantSelectScreen />} />
        <Route path="/select-product" element={<ProductSelectScreen />} />
      </Route>

      <Route element={<RequireAuth />}>
        <Route element={<AppShell />}>
          <Route element={<RequireRole />}>
            <Route path="/dashboard" element={<DashboardGlobal />} />

            <Route path="/admin/tenants" element={<TenantsManagement />} />

            <Route path="/products" element={<ProductsList />} />
            <Route path="/products/new" element={<CreateProductForm />} />
            <Route path="/products/:id" element={<ProductDashboard />} />
            <Route path="/products/:id/detail" element={<ProductDetail />} />
            <Route path="/products/:id/modules" element={<ModulesPage />} />

            <Route path="/content" element={<EditorialDashboard />} />
            <Route path="/content/list" element={<ContentDataGrid />} />
            <Route path="/content/workflow" element={<WorkflowBoard />} />
            <Route path="/content/:id/editor" element={<ContentEditor />} />
            <Route path="/content/:id/preview" element={<ResponsivePreviewFrame />} />
            <Route path="/content/:id/publish" element={<PublishPanel />} />
            <Route path="/content/:id/versions" element={<VersionsPage />} />
            <Route path="/content/:id/compare" element={<VersionCompareView />} />

            <Route path="/assets" element={<AssetLibrary />} />
            <Route path="/assets/upload" element={<AssetUploadScreen />} />
            <Route path="/assets/picker" element={<AssetPicker />} />
            <Route path="/assets/tags" element={<AssetTagManager />} />
            <Route path="/assets/:slug" element={<AssetDetail />} />
            <Route path="/assets/:slug/metadata" element={<AssetMetadataForm />} />
            <Route path="/assets/:slug/usage" element={<AssetUsageScreen />} />

            <Route path="/forms" element={<FormsDashboard />} />
            <Route path="/forms/list" element={<FormsList />} />
            <Route path="/forms/new" element={<FormBuilder />} />
            <Route path="/forms/preview" element={<FormPreviewFrame />} />
            <Route path="/forms/submissions" element={<SubmissionTable />} />
            <Route path="/forms/submissions/:id" element={<SubmissionDetails />} />
            <Route path="/forms/analytics" element={<BasicFormAnalytics />} />
            <Route path="/forms/publication" element={<PublicationPanel />} />
            <Route path="/forms/:slug" element={<FormBuilder />} />

            <Route path="/analytics" element={<AnalyticsOverview />} />
            <Route path="/analytics/health" element={<ProductHealthPanel />} />
            <Route path="/analytics/content" element={<ContentAnalytics />} />
            <Route path="/analytics/forms" element={<FormAnalyticsModule />} />
            <Route path="/analytics/channels" element={<ChannelBreakdown />} />
            <Route path="/analytics/reports" element={<ReportGrid />} />
            <Route path="/analytics/trends" element={<TrendCards />} />
            <Route path="/analytics/states" element={<AnalyticsStates />} />

            <Route path="/knowledge" element={<KnowledgeOverview />} />
            <Route path="/knowledge/graph" element={<GraphCanvasView />} />
            <Route path="/knowledge/relationships" element={<RelationshipExplorer />} />
            <Route path="/knowledge/entities/:id" element={<EntityDetails />} />
            <Route path="/knowledge/search" element={<EntitySearch />} />
            <Route path="/knowledge/orphans" element={<OrphanEntityTable />} />
            <Route path="/knowledge/insights" element={<KnowledgeInsights />} />

            <Route path="/settings" element={<SettingsOverview />} />
            <Route path="/settings/product" element={<ProductSettings />} />
            <Route path="/settings/tenant" element={<TenantSettings />} />
            <Route path="/settings/permissions" element={<PermissionMatrixView />} />
            <Route path="/settings/roles" element={<RoleManagement />} />
            <Route path="/settings/access-preview" element={<AccessPreviewPanel />} />
            <Route path="/settings/security" element={<SecuritySettingsPanel />} />

            <Route path="/users" element={<UserTable />} />
            <Route path="/users/invite" element={<InviteUserDrawer />} />
            <Route path="/users/:id" element={<UserDetailPanel />} />

            <Route path="/audit" element={<AuditTimeline />} />
            <Route path="/audit/:id" element={<AuditEventDetail />} />
          </Route>
        </Route>
      </Route>

      <Route path="*" element={<Navigate to="/dashboard" replace />} />
    </Routes>
  );
}
