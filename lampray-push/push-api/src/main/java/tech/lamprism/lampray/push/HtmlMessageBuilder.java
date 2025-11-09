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

package tech.lamprism.lampray.push;

import com.google.common.html.HtmlEscapers;
import com.google.common.io.BaseEncoding;
import org.apache.commons.lang3.tuple.Pair;

import java.text.MessageFormat;
import java.util.List;

/**
 * @author RollW
 */
public class HtmlMessageBuilder implements PushMessageBuilder {
    private String title;
    private StringBuilder content;

    public HtmlMessageBuilder() {
        this.content = new StringBuilder();
    }

    @Override
    public HtmlMessageBuilder setTitle(String title) {
        this.title = title;
        return this;
    }

    @Override
    public HtmlMessageBuilder clearContent() {
        this.content = new StringBuilder();
        return this;
    }

    @Override
    public PushMessageBuilder appendParagraph(String paragraph) {
        content.append("<p>").append(paragraph).append("</p>");
        return this;
    }

    @Override
    public PushMessageBuilder appendParagraphTitle(String paragraphTitle, ParagraphTitleLevel level) {
        content.append("<h").append(level.getLevel())
                .append(">").append(paragraphTitle)
                .append("</h").append(level.getLevel())
                .append(">");
        return this;
    }

    @Override
    public HtmlMessageBuilder appendContent(String content) {
        return appendContent(content, false);
    }

    private HtmlMessageBuilder append(String content) {
        this.content.append(content);
        return this;
    }

    @Override
    public HtmlMessageBuilder appendContent(String content, boolean escape) {
        if (escape) {
            content = escapeHtml(content);
        }
        return append(content);
    }


    @Override
    public HtmlMessageBuilder appendContent(String content, Object... args) {
        this.content.append(MessageFormat.format(content, args));
        return this;
    }

    @Override
    public PushMessageBuilder breakLine() {
        content.append("<br/>");
        return this;
    }

    @Override
    public HtmlMessageBuilder appendLink(String link, String linkText) {
        content.append("<a href=\"").append(link)
                .append("\">").append(linkText)
                .append("</a>");
        return this;
    }

    @Override
    public HtmlMessageBuilder appendImage(byte[] image,
                                          String imageText) {
        String base64 = BaseEncoding.base64()
                .encode(image);
        content.append("<img src=\"data:image/png;base64,")
                .append(base64)
                .append("\" alt=\"").append(imageText)
                .append("\" />");
        return this;
    }

    @Override
    public HtmlMessageBuilder appendImage(String imageUrl,
                                          String imageText) {
        content.append("<img src=\"").append(imageUrl)
                .append("\" alt=\"").append(imageText)
                .append("\" />");
        return this;
    }

    @Override
    public PushMessageBody build() {
        return new SimplePushMessageBody(
                title, content.toString(), MessageMimeType.HTML
        );
    }

    public HtmlMessageBuilder appendNode(Node node) {
        appendOnNode(node);
        return this;
    }

    private void appendOnNode(Node node) {
        content.append("<").append(node.tag);
        appendAttributes(node.attributes);
        content.append(">");
        if (node.text != null) {
            content.append(node.text);
        }
        if (node.children != null) {
            for (Node child : node.children) {
                appendOnNode(child);
            }
        }
        content.append("</").append(node.tag).append(">");
    }

    private void appendAttribute(String name, String value) {
        content.append(" ").append(name).append("=\"")
                .append(value)
                .append("\"");
    }

    private void appendAttributes(List<Pair<String, String>> attributes) {
        if (attributes == null) {
            return;
        }
        for (Pair<String, String> attribute : attributes) {
            appendAttribute(attribute.getLeft(), attribute.getRight());
        }
    }

    private static String escapeHtml(String content) {
        return HtmlEscapers.htmlEscaper().escape(content);
    }

    public record Node(
            String tag,
            String text,
            Node[] children,
            List<Pair<String, String>> attributes
    ) {
    }
}
