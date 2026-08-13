'use client';

import React, { useEffect, useState, use } from 'react';
import { useRouter } from 'next/navigation';
import { adminVocabularyApi } from '@/features/admin/services/api';
import { AdminVocabularyRequest } from '@/features/admin/types/vocabulary';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Label } from '@/components/ui/Label';
import { Textarea } from '@/components/ui/Textarea';
import { Loader2, ArrowLeft, Save, Plus, Trash2 } from 'lucide-react';

export default function VocabularyEditPage({ params }: { params: Promise<{ id: string }> }) {
  const router = useRouter();
  const { id } = use(params);
  const isNew = id === 'new';

  const [loading, setLoading] = useState(!isNew);
  const [saving, setSaving] = useState(false);
  const [formData, setFormData] = useState<AdminVocabularyRequest>({
    word: '',
    ipaUk: '',
    ipaUs: '',
    cefrLevel: '',
    partOfSpeech: '',
    status: 'DRAFT',
    definitions: [],
    examples: [],
    relations: [],
    categoryIds: [],
    tags: []
  });

  useEffect(() => {
    if (!isNew) {
      adminVocabularyApi.getVocabulary(id)
        .then(res => {
          setFormData({
            word: res.word,
            ipaUk: res.ipaUk || '',
            ipaUs: res.ipaUs || '',
            cefrLevel: res.cefrLevel || '',
            partOfSpeech: res.partOfSpeech || '',
            status: res.status || 'DRAFT',
            definitions: res.definitions || [],
            examples: res.examples || [],
            relations: res.relations || [],
            categoryIds: res.categories || [],
            tags: res.tags || []
          });
        })
        .catch(err => {
          console.error(err);
          alert('Failed to load vocabulary');
        })
        .finally(() => setLoading(false));
    }
  }, [id, isNew]);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
  };

  const handleSave = async () => {
    if (!formData.word.trim()) {
      alert('Word is required');
      return;
    }

    setSaving(true);
    try {
      if (isNew) {
        await adminVocabularyApi.createVocabulary(formData);
      } else {
        await adminVocabularyApi.updateVocabulary(id, formData);
      }
      router.push('/admin/vocabulary/list');
    } catch (error) {
      console.error(error);
      const message = error instanceof Error ? error.message : 'Failed to save vocabulary';
      const responseMessage = error && typeof error === 'object' && 'response' in error ? (error.response as { data?: { message?: string } })?.data?.message : undefined;
      alert(responseMessage || message);
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return <div className="p-8 text-center animate-pulse text-indigo-500">Loading...</div>;
  }

  return (
    <div className="p-8 max-w-4xl mx-auto space-y-8">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-4">
          <Button variant="ghost" size="icon" onClick={() => router.push('/admin/vocabulary/list')}>
            <ArrowLeft />
          </Button>
          <h1 className="text-3xl font-bold text-slate-800 dark:text-white">
            {isNew ? 'Create Vocabulary' : 'Edit Vocabulary'}
          </h1>
        </div>
        <Button onClick={handleSave} disabled={saving} className="gap-2 px-6">
          {saving ? <Loader2 className="animate-spin" /> : <Save size={18} />}
          Save
        </Button>
      </div>

      <div className="bg-white dark:bg-slate-900 rounded-2xl shadow-sm border p-6 space-y-6">
        <h2 className="text-xl font-bold border-b pb-2">Basic Info</h2>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div className="space-y-2">
            <Label>Word <span className="text-red-500">*</span></Label>
            <Input name="word" value={formData.word} onChange={handleChange} placeholder="e.g., apple" />
          </div>
          <div className="space-y-2">
            <Label>Part of Speech</Label>
            <Input name="partOfSpeech" value={formData.partOfSpeech} onChange={handleChange} placeholder="e.g., noun" />
          </div>
          <div className="space-y-2">
            <Label>CEFR Level</Label>
            <select 
              name="cefrLevel" 
              value={formData.cefrLevel} 
              onChange={handleChange}
              className="w-full border rounded-md px-3 py-2 bg-transparent dark:border-slate-800"
            >
              <option value="">Select Level</option>
              <option value="A1">A1</option>
              <option value="A2">A2</option>
              <option value="B1">B1</option>
              <option value="B2">B2</option>
              <option value="C1">C1</option>
              <option value="C2">C2</option>
            </select>
          </div>
          <div className="space-y-2">
            <Label>Status</Label>
            <select 
              name="status" 
              value={formData.status} 
              onChange={handleChange}
              className="w-full border rounded-md px-3 py-2 bg-transparent dark:border-slate-800"
            >
              <option value="DRAFT">Draft</option>
              <option value="PUBLISHED">Published</option>
            </select>
          </div>
          <div className="space-y-2">
            <Label>IPA UK</Label>
            <Input name="ipaUk" value={formData.ipaUk} onChange={handleChange} placeholder="/ˈæp.əl/" />
          </div>
          <div className="space-y-2">
            <Label>IPA US</Label>
            <Input name="ipaUs" value={formData.ipaUs} onChange={handleChange} placeholder="/ˈæp.əl/" />
          </div>
        </div>
      </div>

      <div className="bg-white dark:bg-slate-900 rounded-2xl shadow-sm border p-6 space-y-6">
        <div className="flex justify-between items-center border-b pb-2">
          <h2 className="text-xl font-bold">Definitions</h2>
          <Button 
            variant="outline" 
            size="sm"
            onClick={() => setFormData(prev => ({
              ...prev,
              definitions: [...prev.definitions, { definition: '', displayOrder: prev.definitions.length }]
            }))}
          >
            <Plus size={16} className="mr-2" /> Add Definition
          </Button>
        </div>

        <div className="space-y-4">
          {formData.definitions.map((def, index) => (
            <div key={index} className="p-4 border rounded-xl relative space-y-4 bg-slate-50 dark:bg-slate-950/50">
              <Button 
                variant="ghost" 
                size="icon" 
                className="absolute top-2 right-2 text-red-500 hover:text-red-600"
                onClick={() => setFormData(prev => ({
                  ...prev,
                  definitions: prev.definitions.filter((_, i) => i !== index)
                }))}
              >
                <Trash2 size={16} />
              </Button>
              <div className="space-y-2 pr-10">
                <Label>Definition (English)</Label>
                <Textarea 
                  value={def.definition}
                  onChange={(e) => {
                    const newDefs = [...formData.definitions];
                    newDefs[index].definition = e.target.value;
                    setFormData({ ...formData, definitions: newDefs });
                  }}
                  placeholder="The round fruit of a tree of the rose family..."
                />
              </div>
              <div className="space-y-2">
                <Label>Vietnamese Translation</Label>
                <Input 
                  value={def.vietnameseDefinition || ''}
                  onChange={(e) => {
                    const newDefs = [...formData.definitions];
                    newDefs[index].vietnameseDefinition = e.target.value;
                    setFormData({ ...formData, definitions: newDefs });
                  }}
                  placeholder="Quả táo"
                />
              </div>
            </div>
          ))}
          {formData.definitions.length === 0 && (
            <p className="text-center text-slate-500 py-4">No definitions added yet.</p>
          )}
        </div>
      </div>
    </div>
  );
}
