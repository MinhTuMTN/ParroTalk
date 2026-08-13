"use client";

import React, { useState, useEffect } from 'react';
import { AdminCategoryDto, AdminCategoryCreateRequest, AdminCategoryUpdateRequest } from '../types';
import { categoryService } from '../services/categoryService';
import CategoryEditorModal from './CategoryEditorModal';

export default function CategoryList() {
    const [categories, setCategories] = useState<AdminCategoryDto[]>([]);
    const [loading, setLoading] = useState(true);
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [editingCategory, setEditingCategory] = useState<AdminCategoryDto | null>(null);

    useEffect(() => {
        fetchTree();
    }, []);

    const fetchTree = async () => {
        setLoading(true);
        try {
            const tree = await categoryService.getCategoryTree();
            setCategories(tree);
        } catch (error) {
            console.error("Failed to load category tree", error);
        } finally {
            setLoading(false);
        }
    };

    const handleSave = async (data: AdminCategoryCreateRequest) => {
        if (editingCategory) {
            await categoryService.updateCategory(editingCategory.id, data as AdminCategoryUpdateRequest);
        } else {
            await categoryService.createCategory(data);
        }
        fetchTree(); // Refresh list
    };

    const handleDelete = async (id: string) => {
        if (window.confirm('Are you sure you want to delete this category?')) {
            try {
                await categoryService.deleteCategory(id);
                fetchTree();
            } catch (err) {
                let message = 'Failed to delete category';
                if (err instanceof Error) {
                    message = err.message;
                }
                if (err && typeof err === 'object' && 'response' in err) {
                    const response = (err as { response?: { data?: { message?: string } } }).response;
                    if (response?.data?.message) {
                        message = response.data.message;
                    }
                }
                alert(message);
            }
        }
    };

    const renderTree = (items: AdminCategoryDto[], level = 0) => {
        return items.map(item => (
            <React.Fragment key={item.id}>
                <tr className="border-b border-gray-200 dark:border-gray-700 hover:bg-gray-50 dark:hover:bg-gray-800 transition-colors">
                    <td className="py-3 px-4" style={{ paddingLeft: `${level * 1.5 + 1}rem` }}>
                        <div className="flex items-center gap-2">
                            {item.children && item.children.length > 0 && (
                                <span className="text-gray-400">▾</span>
                            )}
                            <span className="font-medium">{item.name}</span>
                        </div>
                    </td>
                    <td className="py-3 px-4 text-gray-500">{item.slug}</td>
                    <td className="py-3 px-4">
                        <span className={`px-2 py-1 text-xs rounded-full ${item.status === 'ACTIVE' ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-700'}`}>
                            {item.status}
                        </span>
                    </td>
                    <td className="py-3 px-4 text-gray-500">{item.lessonsCount}</td>
                    <td className="py-3 px-4 text-right">
                        <button onClick={() => { setEditingCategory(item); setIsModalOpen(true); }} className="text-indigo-600 hover:text-indigo-900 mr-3">Edit</button>
                        <button onClick={() => handleDelete(item.id)} className="text-red-600 hover:text-red-900">Delete</button>
                    </td>
                </tr>
                {item.children && item.children.length > 0 && renderTree(item.children, level + 1)}
            </React.Fragment>
        ));
    };

    return (
        <div className="bg-white dark:bg-gray-900 shadow rounded-lg overflow-hidden border border-gray-200 dark:border-gray-800">
            <div className="p-4 border-b border-gray-200 dark:border-gray-800 flex justify-between items-center bg-gray-50 dark:bg-gray-800/50">
                <input 
                    type="text" 
                    placeholder="Search categories..." 
                    className="px-4 py-2 border rounded-md dark:bg-gray-950 dark:border-gray-700 w-64 focus:outline-none focus:ring-2 focus:ring-indigo-500"
                />
                <button onClick={() => { setEditingCategory(null); setIsModalOpen(true); }} className="bg-indigo-600 hover:bg-indigo-700 text-white px-4 py-2 rounded-md font-medium transition-colors">
                    Add Category
                </button>
            </div>
            
            <div className="overflow-x-auto">
                <table className="w-full text-left text-sm text-gray-700 dark:text-gray-300">
                    <thead className="bg-gray-50 dark:bg-gray-800/80 text-gray-600 dark:text-gray-400 font-semibold border-b border-gray-200 dark:border-gray-700">
                        <tr>
                            <th className="py-3 px-4">Name</th>
                            <th className="py-3 px-4">Slug</th>
                            <th className="py-3 px-4">Status</th>
                            <th className="py-3 px-4">Lessons</th>
                            <th className="py-3 px-4 text-right">Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        {loading ? (
                            <tr>
                                <td colSpan={5} className="py-8 text-center text-gray-500">
                                    <div className="flex justify-center items-center space-x-2">
                                        <div className="w-4 h-4 rounded-full animate-pulse bg-indigo-600"></div>
                                        <div className="w-4 h-4 rounded-full animate-pulse bg-indigo-600"></div>
                                        <div className="w-4 h-4 rounded-full animate-pulse bg-indigo-600"></div>
                                    </div>
                                </td>
                            </tr>
                        ) : categories.length === 0 ? (
                            <tr>
                                <td colSpan={5} className="py-8 text-center text-gray-500">
                                    No categories found. Create your first category!
                                </td>
                            </tr>
                        ) : (
                            renderTree(categories)
                        )}
                    </tbody>
                </table>
            </div>
            
            <CategoryEditorModal 
                isOpen={isModalOpen}
                onClose={() => setIsModalOpen(false)}
                onSave={handleSave}
                category={editingCategory}
                categories={categories}
            />
        </div>
    );
}
