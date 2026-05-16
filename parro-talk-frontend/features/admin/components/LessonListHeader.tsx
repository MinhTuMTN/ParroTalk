import { Plus } from "lucide-react";
import Link from "next/link";
import Button from "@/components/ui/Button";

export default function LessonListHeader() {
  return (
    <div className="mb-4 flex flex-col gap-6 md:flex-row md:items-end md:justify-between">
      <div>
        <h1 className="text-3xl font-bold tracking-tight">Lesson Management</h1>
        <p className="mt-2 max-w-2xl italic">
          Organize, publish, and curate your language learning curriculum from
          one place.
        </p>
      </div>
      <Link href="/upload">
        <Button leftIcon={<Plus className="h-4 w-4" />}>Create Lesson</Button>
      </Link>
    </div>
  );
}
