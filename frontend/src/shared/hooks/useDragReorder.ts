import { useRef } from "react";
import { useDrag, useDrop } from "react-dnd";

type DragItem = { index: number };

/**
 * Hook de reordenação por arrastar (Sprint 13, Tarefa F) — mesmo padrão de
 * `useDrag`/`useDrop` já usado em `WFCard`/`WFLane` (domínio `content`), mas
 * para listas ordenáveis (arrastar um item para nova posição), não troca
 * entre colunas. `onHoverReorder` atualiza a ordem visual a cada item
 * sobreposto; `onDrop` só dispara quando o usuário solta, para persistir.
 */
export function useDragReorder({ dragType, index, onHoverReorder, onDrop }: {
  dragType: string;
  index: number;
  onHoverReorder: (fromIndex: number, toIndex: number) => void;
  onDrop?: () => void;
}) {
  const ref = useRef<HTMLDivElement>(null);

  const [{ isDragging }, drag] = useDrag(() => ({
    type: dragType,
    item: (): DragItem => ({ index }),
    collect: (monitor) => ({ isDragging: monitor.isDragging() }),
    end: () => onDrop?.(),
  }), [dragType, index, onDrop]);

  const [{ isOver }, drop] = useDrop<DragItem, void, { isOver: boolean }>(() => ({
    accept: dragType,
    hover: (item) => {
      if (item.index === index) return;
      onHoverReorder(item.index, index);
      item.index = index;
    },
    collect: (monitor) => ({ isOver: monitor.isOver() }),
  }), [dragType, index, onHoverReorder]);

  drag(ref);
  drop(ref);

  return { ref, isDragging, isOver };
}
