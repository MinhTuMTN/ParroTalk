'use client';

import React, { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { adminVocabularyApi } from '@/features/admin/services/api';
import { AdminVocabularyListDto, PageResponse } from '@/features/admin/types/vocabulary';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/Table';
import { Badge } from '@/components/ui/Badge';
import { Loader2, Plus, Search, Edit, Trash2 } from 'lucide-react';

export default function VocabularyListPage() {
  const router = useRouter();
  const [data, setData] = useState<PageResponse<AdminVocabularyListDto> | null>(null);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [level, setLevel] = useState('');

  const fetchData = async () => {
    setLoading(true);
    try {
      const res = await adminVocabularyApi.getVocabularies(0, 20, search, level);
      setData(res);
    } catch (error) {
      console.error('Failed to fetch vocabularies', error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    const delayDebounceFn = setTimeout(() => {
      fetchData();
    }, 500);
    return () => clearTimeout(delayDebounceFn);
  }, [search, level]);

  const handleDelete = async (id: string) => {
    if (confirm('Are you sure you want to delete this vocabulary?')) {
      try {
        await adminVocabularyApi.deleteVocabulary(id);
        fetchData();
      } catch (error) {
        console.error('Failed to delete', error);
        alert('Delete failed');
      }
    }
  };

  return (
    <div className="p-8 max-w-7xl mx-auto space-y-6">
      <div className="flex justify-between items-center">
        <h1 className="text-3xl font-bold text-slate-800 dark:text-white">Vocabulary Management</h1>
        <Button onClick={() => router.push('/admin/vocabulary/edit/new')} className="gap-2">
          <Plus size={16} /> Add New
        </Button>
      </div>

      <div className="flex gap-4 p-4 bg-white dark:bg-slate-900 rounded-xl shadow-sm">
        <div className="relative flex-1">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" size={18} />
          <Input 
            placeholder="Search word..." 
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="pl-10"
          />
        </div>
        <select 
          className="border rounded-md px-3 py-2 bg-transparent dark:border-slate-800"
          value={level}
          onChange={(e) => setLevel(e.target.value)}
        >
          <option value="">All Levels</option>
          <option value="A1">A1</option>
          <option value="A2">A2</option>
          <option value="B1">B1</option>
          <option value="B2">B2</option>
          <option value="C1">C1</option>
          <option value="C2">C2</option>
        </select>
      </div>

      <div className="bg-white dark:bg-slate-900 rounded-xl shadow-sm border overflow-hidden">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Word</TableHead>
              <TableHead>POS</TableHead>
              <TableHead>Level</TableHead>
              <TableHead>Defs</TableHead>
              <TableHead>Exs</TableHead>
              <TableHead>Audio</TableHead>
              <TableHead>Status</TableHead>
              <TableHead className="text-right">Actions</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {loading ? (
              <TableRow>
                <TableCell colSpan={8} className="h-32 text-center">
                  <Loader2 className="animate-spin mx-auto text-indigo-500" />
                </TableCell>
              </TableRow>
            ) : data?.content.length === 0 ? (
              <TableRow>
                <TableCell colSpan={8} className="h-32 text-center text-slate-500">
                  No vocabulary found.
                </TableCell>
              </TableRow>
            ) : (
              data?.content.map((vocab) => (
                <TableRow key={vocab.id}>
                  <TableCell className="font-bold">{vocab.word}</TableCell>
                  <TableCell>{vocab.partOfSpeech || '-'}</TableCell>
                  <TableCell>
                    {vocab.cefrLevel ? <Badge variant="outline">{vocab.cefrLevel}</Badge> : '-'}
                  </TableCell>
                  <TableCell>{vocab.definitionsCount}</TableCell>
                  <TableCell>{vocab.examplesCount}</TableCell>
                  <TableCell>{vocab.hasAudio ? '✅' : '❌'}</TableCell>
                  <TableCell>
                    <Badge className={vocab.status === 'PUBLISHED' ? 'bg-emerald-500' : 'bg-slate-400'}>
                      {vocab.status}
                    </Badge>
                  </TableCell>
                  <TableCell className="text-right space-x-2">
                    <Button variant="ghost" size="icon" onClick={() => router.push(`/admin/vocabulary/edit/${vocab.id}`)}>
                      <Edit size={16} />
                    </Button>
                    <Button variant="ghost" size="icon" className="text-red-500 hover:text-red-600" onClick={() => handleDelete(vocab.id)}>
                      <Trash2 size={16} />
                    </Button>
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
        
        {data && data.totalPages > 1 && (
          <div className="p-4 border-t flex justify-center gap-2">
            <span className="text-sm text-slate-500">Showing {data.content.length} of {data.totalElements}</span>
          </div>
        )}
      </div>
    </div>
  );
}
