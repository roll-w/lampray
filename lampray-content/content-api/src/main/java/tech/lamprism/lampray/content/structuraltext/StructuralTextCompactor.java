/*
 * Copyright (C) 2023-2025 RollW
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package tech.lamprism.lampray.content.structuraltext;

import tech.lamprism.lampray.content.structuraltext.element.Blockquote;
import tech.lamprism.lampray.content.structuraltext.element.Bold;
import tech.lamprism.lampray.content.structuraltext.element.CodeBlock;
import tech.lamprism.lampray.content.structuraltext.element.Document;
import tech.lamprism.lampray.content.structuraltext.element.Heading;
import tech.lamprism.lampray.content.structuraltext.element.Highlight;
import tech.lamprism.lampray.content.structuraltext.element.HorizontalDivider;
import tech.lamprism.lampray.content.structuraltext.element.Image;
import tech.lamprism.lampray.content.structuraltext.element.InlineCode;
import tech.lamprism.lampray.content.structuraltext.element.Italic;
import tech.lamprism.lampray.content.structuraltext.element.Link;
import tech.lamprism.lampray.content.structuraltext.element.ListBlock;
import tech.lamprism.lampray.content.structuraltext.element.ListItem;
import tech.lamprism.lampray.content.structuraltext.element.Math;
import tech.lamprism.lampray.content.structuraltext.element.Mention;
import tech.lamprism.lampray.content.structuraltext.element.Paragraph;
import tech.lamprism.lampray.content.structuraltext.element.StrikeThrough;
import tech.lamprism.lampray.content.structuraltext.element.Table;
import tech.lamprism.lampray.content.structuraltext.element.TableCell;
import tech.lamprism.lampray.content.structuraltext.element.TableRow;
import tech.lamprism.lampray.content.structuraltext.element.Text;
import tech.lamprism.lampray.content.structuraltext.element.Underline;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Utility for compacting StructuralText and comparing by the compacted result.
 *
 * @author RollW
 */
public final class StructuralTextCompactor {
    private StructuralTextCompactor() {
    }

    /**
     * Structurally compact the given StructuralText and return a new StructuralText instance.
     */
    public static StructuralText compact(StructuralText root) {
        if (root == null) {
            return null;
        }
        return compressNode(root);
    }

    /**
     * Compare two StructuralText instances by their compacted structures.
     */
    public static boolean equalsCompacted(StructuralText a, StructuralText b) {
        if (a == b) return true;
        StructuralText ca = compact(a);
        StructuralText cb = compact(b);
        return Objects.equals(ca, cb);
    }

    private static StructuralText compressNode(StructuralText node) {
        StructuralTextType type = node.getType();
        List<StructuralText> compressedChildren = compressChildren(node.getChildren());

        switch (type) {
            case DOCUMENT: {
                // Flatten nested DOCUMENTs (redundant main nodes)
                List<StructuralText> flattened = new ArrayList<>();
                for (StructuralText ch : compressedChildren) {
                    if (ch.getType() == StructuralTextType.DOCUMENT) {
                        flattened.addAll(ch.getChildren());
                    } else {
                        flattened.add(ch);
                    }
                }
                return new Document(flattened);
            }
            case PARAGRAPH:
                return new Paragraph(compressedChildren);
            case LIST:
                return rebuildListBlock((ListBlock) node, compressedChildren);
            case LIST_ITEM:
                return new ListItem(node.getContent(), compressedChildren);
            case HEADING:
                return rebuildHeading((Heading) node, compressedChildren);
            case BLOCKQUOTE:
                return new Blockquote(node.getContent(), compressedChildren);
            case CODE_BLOCK:
                return rebuildCodeBlock((CodeBlock) node, compressedChildren);
            case INLINE_CODE:
                return new InlineCode(node.getContent(), compressedChildren);
            case BOLD:
                // Note: Bold requires non-empty content or children; if both empty after compression, return empty Text to be dropped later.
                if (isBlank(node.getContent()) && compressedChildren.isEmpty()) {
                    return new Text("");
                }
                return new Bold(node.getContent(), compressedChildren);
            case ITALIC:
                return new Italic(node.getContent(), compressedChildren);
            case STRIKETHROUGH:
                return new StrikeThrough(node.getContent(), compressedChildren);
            case UNDERLINE:
                return new Underline(node.getContent(), compressedChildren);
            case HIGHLIGHT:
                return rebuildHighlight((Highlight) node, compressedChildren);
            case TEXT:
                return new Text(node.getContent(), compressedChildren);
            case LINK:
                return rebuildLink((Link) node, compressedChildren);
            case IMAGE:
                return rebuildImage((Image) node, compressedChildren);
            case TABLE:
                return new Table(node.getContent(), compressedChildren);
            case TABLE_ROW:
                return new TableRow(node.getContent(), compressedChildren);
            case TABLE_CELL:
                return new TableCell(node.getContent(), compressedChildren);
            case HORIZONTAL_DIVIDER:
                return HorizontalDivider.INSTANCE;
            case MATH: {
                Math math = (Math) node;
                return new Math(math.getContent(), math.getDisplay());
            }
            case MENTION:
                return rebuildMention((Mention) node, compressedChildren);
            default:
                // fallback to original when unknown
                return node;
        }
    }

    private static List<StructuralText> compressChildren(List<StructuralText> children) {
        if (children == null || children.isEmpty()) {
            return Collections.emptyList();
        }
        List<StructuralText> result = new ArrayList<>(children.size());
        for (StructuralText child : children) {
            if (child == null) continue;
            StructuralText cn = compressNode(child);
            if (isDroppable(cn)) {
                // drop meaningless empty leaves like empty TEXT, empty formatting wrappers
                continue;
            }
            if (!result.isEmpty()) {
                StructuralText last = result.get(result.size() - 1);
                // merge TEXT by concatenation
                if (last.getType() == StructuralTextType.TEXT && cn.getType() == StructuralTextType.TEXT) {
                    result.set(result.size() - 1, mergeText((Text) last, (Text) cn));
                    continue;
                }
                // merge if shallow-equal (same type + attributes + content), by concatenating children
                if (shallowEquals(last, cn)) {
                    result.set(result.size() - 1, mergeSameHeader(last, cn));
                    continue;
                }
            }
            result.add(cn);
        }
        return result;
    }

    private static boolean isDroppable(StructuralText node) {
        if (node == null) return true;
        // Drop empty TEXT
        if (node.getType() == StructuralTextType.TEXT) {
            return isBlank(node.getContent()) && node.getChildren().isEmpty();
        }
        // Drop empty inline wrappers with no attributes: Bold/Italic/Underline/StrikeThrough/InlineCode/Highlight(without color)
        return switch (node.getType()) {
            case BOLD, ITALIC, UNDERLINE, STRIKETHROUGH, INLINE_CODE ->
                    isBlank(node.getContent()) && node.getChildren().isEmpty();
            case HIGHLIGHT -> {
                Highlight h = (Highlight) node;
                yield isBlank(node.getContent()) && node.getChildren().isEmpty() && h.getColor() == null;
            }
            default -> false;
        };
    }

    private static boolean isBlank(String s) {
        return s == null || s.isEmpty();
    }

    private static Text mergeText(Text a, Text b) {
        String mergedContent = safe(a.getContent()) + safe(b.getContent());
        List<StructuralText> mergedChildren = new ArrayList<>();
        mergedChildren.addAll(a.getChildren());
        mergedChildren.addAll(b.getChildren());
        return new Text(mergedContent, mergedChildren);
    }

    private static StructuralText mergeSameHeader(StructuralText a, StructuralText b) {
        // types and attributes are equal by contract of shallowEquals
        List<StructuralText> mergedChildren = new ArrayList<>();
        mergedChildren.addAll(a.getChildren());
        mergedChildren.addAll(b.getChildren());
        return switch (a.getType()) {
            case PARAGRAPH -> new Paragraph(mergedChildren);
            case LIST -> rebuildListBlock((ListBlock) a, mergedChildren);
            case LIST_ITEM -> new ListItem(a.getContent(), mergedChildren);
            case HEADING -> rebuildHeading((Heading) a, mergedChildren);
            case BLOCKQUOTE -> new Blockquote(a.getContent(), mergedChildren);
            case CODE_BLOCK -> rebuildCodeBlock((CodeBlock) a, mergedChildren);
            case INLINE_CODE -> new InlineCode(a.getContent(), mergedChildren);
            case BOLD -> new Bold(a.getContent(), mergedChildren);
            case ITALIC -> new Italic(a.getContent(), mergedChildren);
            case STRIKETHROUGH -> new StrikeThrough(a.getContent(), mergedChildren);
            case UNDERLINE -> new Underline(a.getContent(), mergedChildren);
            case HIGHLIGHT -> rebuildHighlight((Highlight) a, mergedChildren);
            case LINK -> rebuildLink((Link) a, mergedChildren);
            case IMAGE -> rebuildImage((Image) a, mergedChildren);
            case TABLE -> new Table(a.getContent(), mergedChildren);
            case TABLE_ROW -> new TableRow(a.getContent(), mergedChildren);
            case TABLE_CELL -> new TableCell(a.getContent(), mergedChildren);
            case DOCUMENT -> {
                // normally handled at root, but support here for completeness
                List<StructuralText> flattened = new ArrayList<>();
                for (StructuralText ch : mergedChildren) {
                    if (ch.getType() == StructuralTextType.DOCUMENT) flattened.addAll(ch.getChildren());
                    else flattened.add(ch);
                }
                yield new Document(flattened);
            }
            // TEXT and others should not reach here (TEXT handled separately)
            default -> a;
        };
    }

    private static boolean shallowEquals(StructuralText a, StructuralText b) {
        if (a.getType() != b.getType()) return false;
        switch (a.getType()) {
            case PARAGRAPH, TABLE, TABLE_ROW, TABLE_CELL, BLOCKQUOTE, INLINE_CODE, BOLD, ITALIC, STRIKETHROUGH,
                 UNDERLINE -> {
                return Objects.equals(a.getContent(), b.getContent());
            }
            case LIST -> {
                ListBlock la = (ListBlock) a;
                ListBlock lb = (ListBlock) b;
                return la.getOrdered() == lb.getOrdered() && Objects.equals(a.getContent(), b.getContent());
            }
            case HEADING -> {
                Heading ha = (Heading) a;
                Heading hb = (Heading) b;
                return ha.getLevel() == hb.getLevel() && Objects.equals(a.getContent(), b.getContent());
            }
            case CODE_BLOCK -> {
                CodeBlock ca = (CodeBlock) a;
                CodeBlock cb = (CodeBlock) b;
                return Objects.equals(ca.getLanguage(), cb.getLanguage()) && Objects.equals(a.getContent(), b.getContent());
            }
            case HIGHLIGHT -> {
                Highlight ha = (Highlight) a;
                Highlight hb = (Highlight) b;
                return Objects.equals(ha.getColor(), hb.getColor()) && Objects.equals(a.getContent(), b.getContent());
            }
            case LINK -> {
                Link la = (Link) a;
                Link lb = (Link) b;
                return Objects.equals(la.getHref(), lb.getHref()) && Objects.equals(la.getTitle(), lb.getTitle());
            }
            case IMAGE -> {
                Image ia = (Image) a;
                Image ib = (Image) b;
                return Objects.equals(ia.getSrc(), ib.getSrc())
                        && Objects.equals(ia.getAlt(), ib.getAlt())
                        && Objects.equals(ia.getTitle(), ib.getTitle())
                        && Objects.equals(a.getContent(), b.getContent());
            }
            case MENTION -> {
                Mention ma = (Mention) a;
                Mention mb = (Mention) b;
                return Objects.equals(ma.getUserId(), mb.getUserId()) && Objects.equals(a.getContent(), b.getContent());
            }
            case MATH -> {
                Math ma = (Math) a;
                Math mb = (Math) b;
                return ma.getDisplay() == mb.getDisplay() && Objects.equals(ma.getContent(), mb.getContent());
            }
            case HORIZONTAL_DIVIDER -> {
                return true; // all dividers are identical in header
            }
            case DOCUMENT -> {
                return true;
            }
            case TEXT -> {
                // TEXT handled separately (always merge by concatenation when adjacent)
                return false;
            }
            default -> {
                return false;
            }
        }
    }

    private static ListBlock rebuildListBlock(ListBlock src, List<StructuralText> children) {
        return new ListBlock(src.getOrdered(), src.getContent(), children);
    }

    private static Heading rebuildHeading(Heading src, List<StructuralText> children) {
        return new Heading(src.getLevel(), src.getContent(), children);
    }

    private static CodeBlock rebuildCodeBlock(CodeBlock src, List<StructuralText> children) {
        return new CodeBlock(src.getLanguage(), src.getContent(), children);
    }

    private static Highlight rebuildHighlight(Highlight src, List<StructuralText> children) {
        return new Highlight(src.getColor(), src.getContent(), children);
    }

    private static Link rebuildLink(Link src, List<StructuralText> children) {
        return new Link(src.getHref(), src.getTitle(), children);
    }

    private static Image rebuildImage(Image src, List<StructuralText> children) {
        return new Image(src.getSrc(), src.getAlt(), src.getTitle(), src.getContent(), children);
    }

    private static Mention rebuildMention(Mention src, List<StructuralText> children) {
        return new Mention(src.getUserId(), src.getContent(), children);
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}
