import { Metadata } from "next";
import LibrarySectionListPage from "@/features/admin/components/LibrarySectionListPage";

export const metadata: Metadata = {
  title: "Library Sections | ParroTalk Admin",
  description: "Manage library shelves, category highlights, and program sections",
};

export default function AdminLibrarySectionsPage() {
  return <LibrarySectionListPage />;
}
