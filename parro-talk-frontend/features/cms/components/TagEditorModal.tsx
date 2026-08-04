"use client";

import React, { useState, useEffect } from 'react';
import { AdminTagDto, AdminTagCreateRequest } from '../types';

interface Props {
    isOpen: boolean;
    onClose: () => void;
    onSave: (data: any) => Promise<void>;
    tag?: AdminTagDto | null;
}

export default function TagEditorModal({ isOpen, onClose, onSave, tag }: Props) {
    const [formData, setFormData] = useState<AdminTagCreateRequest & { id?: string }>({
        name: '',
        slug: '',
        color: '#10b981',
        description: '',
        status: 'ACTIVE'
    });
    
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');

    useEffect(() => {
        if (tag) {
            setFormData({
                id: tag.id,
                name: tag.name,
                slug: tag.slug,
                description: tag.description || '',
                color: tag.color || '#10b981',
                status: tag.status
            });
        } else {
            setFormData({
                name: '',
                slug: '',
                description: '',
                color: '#10b981',
                status: 'ACTIVE'
            });
        }
        setError('');
    }, [tag, isOpen]);

    if (!isOpen) return null;

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setError('');
        setLoading(true);
        try {
            const submitData = { ...formData };
            if (!submitData.slug) {
                delete submitData.slug;
            }
            await onSave(submitData);
            onClose();
        } catch (err: any) {
            setError(err?.response?.data?.message || err.message || 'An error occurred while saving.');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm transition-opacity">
            <div className="bg-white dark:bg-gray-900 rounded-xl shadow-2xl w-full max-w-lg flex flex-col overflow-hidden border border-gray-200 dark:border-gray-800 animate-in fade-in zoom-in duration-200">
                <div className="px-6 py-4 border-b border-gray-200 dark:border-gray-800 flex justify-between items-center bg-gray-50 dark:bg-gray-800/50">
                    <h2 className="text-xl font-semibold text-gray-900 dark:text-white">
                        {tag ? 'Edit Tag' : 'Create New Tag'}
                    </h2>
                    <button onClick={onClose} className="text-gray-500 hover:text-gray-700 dark:hover:text-gray-300">
                        <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12"></path></svg>
                    </button>
                </div>
                
                <form onSubmit={handleSubmit} className="p-6 space-y-5">
                    {error && (
                        <div className="bg-red-50 dark:bg-red-900/30 text-red-600 dark:text-red-400 p-4 rounded-lg border border-red-200 dark:border-red-800 text-sm">
                            {error}
                        </div>
                    )}

                    <div className="space-y-2">
                        <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">Name <span className="text-red-500">*</span></label>
                        <input 
                            type="text" 
                            required
                            value={formData.name}
                            onChange={e => setFormData({...formData, name: e.target.value})}
                            className="w-full px-4 py-2 border rounded-md dark:bg-gray-950 dark:border-gray-700 focus:ring-2 focus:ring-indigo-500"
                        />
                    </div>
                    
                    <div className="space-y-2">
                        <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">Slug</label>
                        <input 
                            type="text" 
                            value={formData.slug}
                            placeholder="Auto-generated if empty"
                            onChange={e => setFormData({...formData, slug: e.target.value})}
                            className="w-full px-4 py-2 border rounded-md dark:bg-gray-950 dark:border-gray-700 focus:ring-2 focus:ring-indigo-500"
                        />
                    </div>

                    <div className="space-y-2">
                        <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">Description</label>
                        <textarea 
                            value={formData.description}
                            onChange={e => setFormData({...formData, description: e.target.value})}
                            className="w-full px-4 py-2 border rounded-md dark:bg-gray-950 dark:border-gray-700 focus:ring-2 focus:ring-indigo-500"
                            rows={2}
                        />
                    </div>

                    <div className="grid grid-cols-2 gap-4">
                        <div className="space-y-2">
                            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">Color</label>
                            <div className="flex gap-2 items-center">
                                <input 
                                    type="color" 
                                    value={formData.color}
                                    onChange={e => setFormData({...formData, color: e.target.value})}
                                    className="w-10 h-10 border-0 rounded cursor-pointer p-0"
                                />
                                <input 
                                    type="text"
                                    value={formData.color}
                                    onChange={e => setFormData({...formData, color: e.target.value})}
                                    className="flex-1 px-3 py-2 border rounded-md text-sm dark:bg-gray-950 dark:border-gray-700"
                                />
                            </div>
                        </div>
                        
                        <div className="space-y-2">
                            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">Status</label>
                            <select 
                                value={formData.status}
                                onChange={e => setFormData({...formData, status: e.target.value as any})}
                                className="w-full px-4 py-2 border rounded-md dark:bg-gray-950 dark:border-gray-700 focus:ring-2 focus:ring-indigo-500"
                            >
                                <option value="ACTIVE">Active</option>
                                <option value="INACTIVE">Inactive</option>
                            </select>
                        </div>
                    </div>
                    
                    <div className="pt-4 mt-6 border-t border-gray-200 dark:border-gray-800 flex justify-end gap-3">
                        <button 
                            type="button" 
                            onClick={onClose}
                            className="px-6 py-2 border border-gray-300 dark:border-gray-700 rounded-md font-medium hover:bg-gray-50 dark:hover:bg-gray-800 transition-colors"
                        >
                            Cancel
                        </button>
                        <button 
                            type="submit" 
                            disabled={loading}
                            className="px-6 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-md font-medium transition-colors disabled:opacity-50 flex items-center gap-2"
                        >
                            {loading ? (
                                <><span className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"></span> Saving...</>
                            ) : 'Save Tag'}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}
