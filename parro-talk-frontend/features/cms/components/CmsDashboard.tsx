"use client";

import React, { useState, useEffect } from 'react';
import { AdminCategoryDto, AdminTagDto } from '../types';
import { categoryService } from '../services/categoryService';
import { tagService } from '../services/tagService';

export default function CmsDashboard() {
    const [categories, setCategories] = useState<AdminCategoryDto[]>([]);
    const [tags, setTags] = useState<AdminTagDto[]>([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const loadStats = async () => {
            try {
                // Simplified for MVP, fetching first page, a real dashboard would have a dedicated stats endpoint
                const [treeData, tagsData] = await Promise.all([
                    categoryService.getCategoryTree(),
                    tagService.getTags(0, 1000)
                ]);
                
                // Flatten tree for stats
                const flatten = (items: AdminCategoryDto[]): AdminCategoryDto[] => {
                    let result: AdminCategoryDto[] = [];
                    items.forEach(item => {
                        result.push(item);
                        if (item.children) result = result.concat(flatten(item.children));
                    });
                    return result;
                };
                
                setCategories(flatten(treeData));
                setTags(tagsData.content);
            } catch (error) {
                console.error("Failed to load dashboard stats", error);
            } finally {
                setLoading(false);
            }
        };
        
        loadStats();
    }, []);

    if (loading) {
        return (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
                {[1, 2, 3, 4].map(i => (
                    <div key={i} className="bg-white dark:bg-gray-900 rounded-xl p-6 shadow-sm border border-gray-100 dark:border-gray-800 animate-pulse">
                        <div className="h-4 bg-gray-200 dark:bg-gray-800 rounded w-1/2 mb-4"></div>
                        <div className="h-8 bg-gray-200 dark:bg-gray-800 rounded w-3/4"></div>
                    </div>
                ))}
            </div>
        );
    }

    const activeCategories = categories.filter(c => c.status === 'ACTIVE').length;
    const activeTags = tags.filter(t => t.status === 'ACTIVE').length;
    const unusedCategories = categories.filter(c => c.lessonsCount === 0).length;
    const unusedTags = tags.filter(t => t.lessonsCount === 0).length;

    return (
        <div className="space-y-6">
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
                <div className="bg-white dark:bg-gray-900 rounded-xl p-6 shadow-sm border border-gray-100 dark:border-gray-800">
                    <h3 className="text-gray-500 text-sm font-medium">Total Categories</h3>
                    <p className="text-3xl font-bold text-gray-900 dark:text-white mt-2">{categories.length}</p>
                    <div className="mt-2 text-sm text-green-600 flex items-center gap-1">
                        <span className="w-2 h-2 rounded-full bg-green-500"></span> {activeCategories} Active
                    </div>
                </div>
                
                <div className="bg-white dark:bg-gray-900 rounded-xl p-6 shadow-sm border border-gray-100 dark:border-gray-800">
                    <h3 className="text-gray-500 text-sm font-medium">Total Tags</h3>
                    <p className="text-3xl font-bold text-gray-900 dark:text-white mt-2">{tags.length}</p>
                    <div className="mt-2 text-sm text-green-600 flex items-center gap-1">
                        <span className="w-2 h-2 rounded-full bg-green-500"></span> {activeTags} Active
                    </div>
                </div>
                
                <div className="bg-white dark:bg-gray-900 rounded-xl p-6 shadow-sm border border-gray-100 dark:border-gray-800">
                    <h3 className="text-gray-500 text-sm font-medium">Unused Categories</h3>
                    <p className="text-3xl font-bold text-yellow-600 mt-2">{unusedCategories}</p>
                    <p className="mt-2 text-sm text-gray-500">Not assigned to any lessons</p>
                </div>
                
                <div className="bg-white dark:bg-gray-900 rounded-xl p-6 shadow-sm border border-gray-100 dark:border-gray-800">
                    <h3 className="text-gray-500 text-sm font-medium">Unused Tags</h3>
                    <p className="text-3xl font-bold text-yellow-600 mt-2">{unusedTags}</p>
                    <p className="mt-2 text-sm text-gray-500">Not assigned to any lessons</p>
                </div>
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                <div className="bg-white dark:bg-gray-900 rounded-xl p-6 shadow-sm border border-gray-100 dark:border-gray-800">
                    <h3 className="text-lg font-semibold mb-4 text-gray-900 dark:text-white">Most Used Categories</h3>
                    <div className="space-y-4">
                        {categories.sort((a, b) => b.lessonsCount - a.lessonsCount).slice(0, 5).map(cat => (
                            <div key={cat.id} className="flex justify-between items-center">
                                <span className="font-medium text-gray-700 dark:text-gray-300">{cat.name}</span>
                                <span className="text-sm bg-gray-100 dark:bg-gray-800 text-gray-600 dark:text-gray-400 py-1 px-3 rounded-full">
                                    {cat.lessonsCount} lessons
                                </span>
                            </div>
                        ))}
                    </div>
                </div>
                
                <div className="bg-white dark:bg-gray-900 rounded-xl p-6 shadow-sm border border-gray-100 dark:border-gray-800">
                    <h3 className="text-lg font-semibold mb-4 text-gray-900 dark:text-white">Most Used Tags</h3>
                    <div className="space-y-4">
                        {tags.sort((a, b) => b.lessonsCount - a.lessonsCount).slice(0, 5).map(tag => (
                            <div key={tag.id} className="flex justify-between items-center">
                                <div className="flex items-center gap-2">
                                    {tag.color && <div className="w-3 h-3 rounded-full" style={{backgroundColor: tag.color}}></div>}
                                    <span className="font-medium text-gray-700 dark:text-gray-300">{tag.name}</span>
                                </div>
                                <span className="text-sm bg-gray-100 dark:bg-gray-800 text-gray-600 dark:text-gray-400 py-1 px-3 rounded-full">
                                    {tag.lessonsCount} lessons
                                </span>
                            </div>
                        ))}
                    </div>
                </div>
            </div>
        </div>
    );
}
