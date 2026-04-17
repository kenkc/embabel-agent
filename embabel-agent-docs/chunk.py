from docling.document_converter import DocumentConverter
from docling_core.transforms.chunker import HierarchicalChunker
import json
from pathlib import Path

# Demonstrate chunking with Docling
# Convert and chunk
converter = DocumentConverter()
result = converter.convert("./target/generated-docs/index.pdf")
doc = result.document

chunker = HierarchicalChunker()
chunks = list(chunker.chunk(doc))

# Save as JSONL (one chunk per line)
output_path = Path("./target/index_chunks.jsonl")
with open(output_path, "w", encoding="utf-8") as f:
    for i, chunk in enumerate(chunks):
        print(f"Saving chunk {i} with {len(chunk.text)} characters")
        chunk_dict = {
            "chunk_id": i,
            "text": chunk.text,
            "meta": {
                "doc_items": [str(ref) for ref in chunk.meta.doc_items],
                "headings": chunk.meta.headings,
                "captions": chunk.meta.captions,
            }
        }
        f.write(json.dumps(chunk_dict, ensure_ascii=False) + "\n")

print(f"✓ Saved {len(chunks)} chunks to {output_path}")