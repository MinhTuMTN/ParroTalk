import LessonEditPage from "@/src/screens/lessons/LessonEditPage";

export default async function AdminLessonEditRoute({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;
  return <LessonEditPage id={id} />;
}
