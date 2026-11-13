import { Metadata } from "next";
import CategoryManagementPage from "@/features/cms/components/categories/CategoryManagementPage";

export const metadata: Metadata = {
  title: "Lesson Categories | ParroTalk Admin",
  description: "Manage hierarchical lesson categories, bilingual names, icons, and ordering",
};

export default function CategoriesPage() {
  return <CategoryManagementPage />;
}
