import { useRouter } from "next/navigation";
import { Play } from "lucide-react";
import { useState } from "react";

import { lessonService, Lesson } from "@/features/lesson/services/lessonService";
import Link from "next/link";
import ConfirmationModal from "@/components/ui/ConfirmationModal";

export default function FeaturedLesson({ job }: { job: Lesson }) {
    const router = useRouter();
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [isResetting, setIsResetting] = useState(false);

    // Logic routing
    const href = `/practice/${job.id}`;

    const status = job.progress === 100 ? "DONE" : job.progress > 0 ? "IN_PROGRESS" : "NOT_STARTED";
    const isDone = status === "DONE";

    const handleAction = (e: React.MouseEvent) => {
        if (isDone) {
            e.preventDefault();
            setIsModalOpen(true);
        }
    };

    const handleConfirmReset = async () => {
        setIsResetting(true);
        try {
            await lessonService.resetProgress(job.id);
            router.push(href);
        } catch (err) {
            console.error("Failed to reset progress", err);
            router.push(href);
        } finally {
            setIsResetting(false);
            setIsModalOpen(false);
        }
    };

    return (
        <div className="relative w-full bg-gray-900 rounded-[2rem] overflow-hidden shadow-2xl flex flex-col md:flex-row group mb-8">
            {/* Background Image with Overlay */}
            <div className="absolute inset-0 z-0">
                <img
                    src="https://m.media-amazon.com/images/I/71z9av0Rs1L.jpg"
                    alt="Featured bg"
                    className="w-full h-full object-cover opacity-20 group-hover:opacity-30 transition-opacity duration-700 blur border-0"
                />
                <div className="absolute inset-0 bg-gradient-to-r from-gray-900 via-gray-900/90 to-transparent" />
            </div>

            <div className="relative z-10 flex-1 p-6 sm:p-10 md:p-14 flex flex-col justify-center items-start gap-4 md:gap-6">
                <div className="inline-flex items-center gap-2 px-3 py-1 bg-green-500/20 border border-green-500/30 rounded-full text-green-400 text-[10px] sm:text-xs font-black uppercase tracking-widest">
                    <span className="w-2 h-2 rounded-full bg-green-400 animate-pulse" />
                    New Release
                </div>

                <div className="flex flex-col gap-2 md:gap-3">
                    <div className="flex items-center gap-4 text-[10px] sm:text-xs font-bold text-gray-400 uppercase tracking-widest">
                        <span>Tech</span>
                        <span>•</span>
                        <span>{job.duration} mins</span>
                    </div>
                    <h2 className="text-2xl sm:text-3xl md:text-5xl font-black text-white leading-tight">
                        {job.title}
                    </h2>
                    <p className="text-gray-400 max-w-lg text-sm sm:text-lg line-clamp-2 md:line-clamp-3">
                        Master the nuances of the context from the generated transcription. Specially customized for your growth and progression in global tech environments.
                    </p>
                </div>

                <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-3 sm:gap-4 mt-2 sm:mt-4 w-full sm:w-auto">
                    <Link
                        href={href}
                        onClick={handleAction}
                        className={`px-6 sm:px-8 py-3 sm:py-4 rounded-xl font-black transition-colors flex items-center justify-center gap-2 active:scale-95 text-sm sm:text-base bg-green-500 hover:bg-green-400 text-gray-900 shadow-lg shadow-green-500/20`}
                    >
                        {status === "DONE" ? "Review Lesson" : status === "IN_PROGRESS" ? "Continue Lesson" : "Start Lesson"}
                        <Play className="w-4 h-4 sm:w-5 sm:h-5 fill-current" />
                    </Link>
                    <button className="px-6 sm:px-8 py-3 sm:py-4 rounded-xl font-bold bg-white/10 text-white hover:bg-white/20 transition-colors text-sm sm:text-base">
                        View Details
                    </button>
                </div>
            </div>


            {/* Right side graphical focus */}
            <div className="relative z-10 w-full md:w-2/5 min-h-[300px] hidden md:flex items-center justify-center p-8 lg:p-12">
                <img
                    src="https://m.media-amazon.com/images/I/71z9av0Rs1L.jpg"
                    alt="Cover"
                    className="w-full h-auto max-h-[300px] object-cover rounded-2xl shadow-2xl rotate-2 group-hover:rotate-0 transition-transform duration-500 border border-white/10"
                    style={{ aspectRatio: "16/9" }}
                />
            </div>

            <ConfirmationModal
                isOpen={isModalOpen}
                onClose={() => setIsModalOpen(false)}
                onConfirm={handleConfirmReset}
                isLoading={isResetting}
                title="Study Again?"
                message="You have already completed this lesson. Would you like to start from the beginning? Your current progress will be reset."
                confirmText="Start Over"
            />
        </div>
    )
}

