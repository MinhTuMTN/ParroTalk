import Link from "next/link";
import { Headphones, Play } from "lucide-react";

import { Lesson } from "@/lib/services/lessonService";

export default function LessonCard({ job }: { job: Lesson }) {

    const urlParts = job.fileUrl.split('/');
    const rawName = urlParts[urlParts.length - 1] || "Untitled";
    const title = decodeURIComponent(rawName.substring(rawName.indexOf('_') + 1)).replace(/\.[^/.]+$/, "");

    // Mock category array
    const categories = ["BUSINESS", "DAILY LIFE", "TECH", "ACADEMIC"];
    const hash = job.id.charCodeAt(0) + job.id.charCodeAt(job.id.length - 1);
    const category = categories[hash % categories.length];
    
    const duration = 15 + (hash % 30); // Mock duration between 15 and 44

    const isDone = job.status === "DONE";
    const percent = isDone ? 100 : job.progress;

    return (
        <div className="bg-white border border-gray-100 rounded-[2rem] p-4 shadow-sm hover:shadow-xl hover:border-green-100 transition-all duration-300 group flex flex-col">
            <div className="relative w-full aspect-[4/3] sm:aspect-video rounded-2xl overflow-hidden bg-gray-50 mb-6">
                <img 
                    src="https://m.media-amazon.com/images/I/71z9av0Rs1L.jpg" 
                    alt="thumbnail" 
                    className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
                />
                
                {/* Optional Media Type badge (Audio) */}
                <div className="absolute top-3 right-3 bg-black/60 backdrop-blur-md px-3 py-1.5 rounded-lg flex items-center gap-2 text-white text-xs font-bold">
                    <Headphones className="w-3.5 h-3.5" />
                    AUDIO
                </div>
            </div>

            <div className="flex flex-col flex-1 px-2">
                <div className="flex items-center justify-between text-xs font-black text-gray-400 tracking-widest mb-3 uppercase">
                    <span className="text-green-600 bg-green-50 px-2 py-1 rounded-md">{category}</span>
                    <span className="flex items-center gap-1">
                        <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" /></svg>
                        {duration} mins
                    </span>
                </div>
                
                <h3 className="text-lg sm:text-xl font-bold text-gray-900 mb-4 sm:mb-6 line-clamp-2 leading-tight group-hover:text-green-600 transition-colors">
                    {title || "Custom Listening Lesson"}
                </h3>


                <div className="mt-auto flex flex-col gap-4">
                    <div className="flex flex-col gap-2">
                        <div className="flex items-center justify-between text-xs font-bold text-gray-500">
                            <span>Completion</span>
                            <span>{percent}%</span>
                        </div>
                        <div className="w-full bg-gray-100 rounded-full h-1.5 overflow-hidden">
                            <div 
                                className="bg-green-500 h-full rounded-full transition-all duration-1000 ease-out"
                                style={{ width: `${percent}%` }}
                            />
                        </div>
                    </div>

                    <Link 
                        href={`/practice/${job.id}`} 
                        className={`w-full py-3 rounded-xl font-bold flex items-center justify-center transition-colors active:scale-95 ${
                            isDone 
                            ? "bg-gray-900 text-white hover:bg-gray-800 shadow-md shadow-gray-200" 
                            : "bg-gray-100 text-gray-400 cursor-not-allowed"
                        }`}
                        onClick={e => !isDone && e.preventDefault()}
                    >
                        {isDone ? "Review Lesson" : job.currentStep || "Processing..."}
                    </Link>
                </div>
            </div>
        </div>
    )
}
