import LessonEditPage from "@/features/admin/components/LessonEditPage";

export default async function AdminLessonEditRoute({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;
  return <LessonEditPage id={id} />;
}
