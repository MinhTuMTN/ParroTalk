import type { AdminCategoryDto, CmsItemStatus } from '../../types';

export interface CategoryStats {
  total: number;
  active: number;
  inactive: number;
  withoutLessons: number;
  rootCount: number;
}

export interface FlattenedCategoryOption {
  id: string;
  name: string;
  nameVi: string | null;
  slug: string;
  level: number;
  disabled: boolean;
}

export interface FilteredCategoryTreeResult {
  filteredTree: AdminCategoryDto[];
  autoExpandedIds: Set<string>;
  matchingIds: Set<string>;
}

/**
 * Normalizes English & Vietnamese category names into URL-safe slugs.
 * Matches backend AdminCategoryService.generateSlug exactly.
 */
export function slugifyCategoryName(input: string): string {
  if (!input) return '';
  return input
    .replace(/đ/g, 'd')
    .replace(/Đ/g, 'd')
    .normalize('NFD')
    .replace(/\p{M}+/gu, '')
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '');
}

export function computeCategoryStats(tree: AdminCategoryDto[]): CategoryStats {
  let total = 0;
  let active = 0;
  let inactive = 0;
  let withoutLessons = 0;

  const visit = (nodes: AdminCategoryDto[]) => {
    for (const node of nodes) {
      total += 1;
      if (node.status === 'ACTIVE') {
        active += 1;
      } else {
        inactive += 1;
      }
      if ((node.lessonsCount ?? 0) === 0) {
        withoutLessons += 1;
      }
      if (node.children && node.children.length > 0) {
        visit(node.children);
      }
    }
  };

  visit(tree);

  return {
    total,
    active,
    inactive,
    withoutLessons,
    rootCount: tree.length,
  };
}

export function collectExpandableIds(tree: AdminCategoryDto[]): Set<string> {
  const ids = new Set<string>();
  const visit = (nodes: AdminCategoryDto[]) => {
    for (const node of nodes) {
      if (node.children && node.children.length > 0) {
        ids.add(node.id);
        visit(node.children);
      }
    }
  };
  visit(tree);
  return ids;
}

export function collectDescendantIds(tree: AdminCategoryDto[], targetId: string): Set<string> {
  const result = new Set<string>();

  const collectAll = (node: AdminCategoryDto) => {
    result.add(node.id);
    if (node.children) {
      for (const child of node.children) {
        collectAll(child);
      }
    }
  };

  const findAndCollect = (nodes: AdminCategoryDto[]): boolean => {
    for (const node of nodes) {
      if (node.id === targetId) {
        collectAll(node);
        return true;
      }
      if (node.children && findAndCollect(node.children)) {
        return true;
      }
    }
    return false;
  };

  findAndCollect(tree);
  return result;
}

export function flattenCategoryOptions(
  tree: AdminCategoryDto[],
  excludedIds?: Set<string>
): FlattenedCategoryOption[] {
  const result: FlattenedCategoryOption[] = [];

  const visit = (nodes: AdminCategoryDto[], level: number) => {
    for (const node of nodes) {
      result.push({
        id: node.id,
        name: node.name,
        nameVi: node.nameVi ?? null,
        slug: node.slug,
        level,
        disabled: excludedIds ? excludedIds.has(node.id) : false,
      });
      if (node.children && node.children.length > 0) {
        visit(node.children, level + 1);
      }
    }
  };

  visit(tree, 0);
  return result;
}

export function filterCategoryTree(
  tree: AdminCategoryDto[],
  searchQuery: string,
  statusFilter: 'ALL' | CmsItemStatus
): FilteredCategoryTreeResult {
  const normalizedQuery = searchQuery.trim().toLowerCase();
  const hasSearch = normalizedQuery.length > 0;
  const hasStatusFilter = statusFilter !== 'ALL';

  const autoExpandedIds = new Set<string>();
  const matchingIds = new Set<string>();

  if (!hasSearch && !hasStatusFilter) {
    return {
      filteredTree: tree,
      autoExpandedIds,
      matchingIds,
    };
  }

  const filterNodes = (nodes: AdminCategoryDto[]): AdminCategoryDto[] => {
    const kept: AdminCategoryDto[] = [];

    for (const node of nodes) {
      const filteredChildren = node.children ? filterNodes(node.children) : [];

      const matchesStatus = !hasStatusFilter || node.status === statusFilter;
      const matchesSearch =
        !hasSearch ||
        node.name.toLowerCase().includes(normalizedQuery) ||
        (node.nameVi ? node.nameVi.toLowerCase().includes(normalizedQuery) : false) ||
        node.slug.toLowerCase().includes(normalizedQuery);

      const selfMatches = matchesStatus && matchesSearch;
      if (selfMatches && hasSearch) {
        matchingIds.add(node.id);
      }

      if (selfMatches || filteredChildren.length > 0) {
        if (filteredChildren.length > 0) {
          autoExpandedIds.add(node.id);
        }
        kept.push({
          ...node,
          children: filteredChildren,
        });
      }
    }

    return kept;
  };

  return {
    filteredTree: filterNodes(tree),
    autoExpandedIds,
    matchingIds,
  };
}
