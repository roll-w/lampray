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

import katex from 'katex'

/**
 * Helpers
 */
const escapeReplacements = {
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    '"': '&quot;',
    "'": '&#39;',
};
const getEscapeReplacement = (ch) => escapeReplacements[ch];
const other = {
    codeRemoveIndent: /^(?: {1,4}| {0,3}\t)/gm,
    outputLinkReplace: /\\([\[\]])/g,
    indentCodeCompensation: /^(\s+)(?:```)/,
    beginningSpace: /^\s+/,
    endingHash: /#$/,
    startingSpaceChar: /^ /,
    endingSpaceChar: / $/,
    nonSpaceChar: /[^ ]/,
    newLineCharGlobal: /\n/g,
    tabCharGlobal: /\t/g,
    multipleSpaceGlobal: /\s+/g,
    blankLine: /^[ \t]*$/,
    doubleBlankLine: /\n[ \t]*\n[ \t]*$/,
    blockquoteStart: /^ {0,3}>/,
    blockquoteSetextReplace: /\n {0,3}((?:=+|-+) *)(?=\n|$)/g,
    blockquoteSetextReplace2: /^ {0,3}>[ \t]?/gm,
    listReplaceTabs: /^\t+/,
    listReplaceNesting: /^ {1,4}(?=( {4})*[^ ])/g,
    listIsTask: /^\[[ xX]\] /,
    listReplaceTask: /^\[[ xX]\] +/,
    anyLine: /\n.*\n/,
    hrefBrackets: /^<(.*)>$/,
    tableDelimiter: /[:|]/,
    tableAlignChars: /^\||\| *$/g,
    tableRowBlankLine: /\n[ \t]*$/,
    tableAlignRight: /^ *-+: *$/,
    tableAlignCenter: /^ *:-+: *$/,
    tableAlignLeft: /^ *:-+ *$/,
    startATag: /^<a /i,
    endATag: /^<\/a>/i,
    startPreScriptTag: /^<(pre|code|kbd|script)(\s|>)/i,
    endPreScriptTag: /^<\/(pre|code|kbd|script)(\s|>)/i,
    startAngleBracket: /^</,
    endAngleBracket: />$/,
    pedanticHrefTitle: /^([^'"]*[^\s])\s+(['"])(.*)\2/,
    unicodeAlphaNumeric: /[\p{L}\p{N}]/u,
    escapeTest: /[&<>"']/,
    escapeReplace: /[&<>"']/g,
    escapeTestNoEncode: /[<>"']|&(?!(#\d{1,7}|#[Xx][a-fA-F0-9]{1,6}|\w+);)/,
    escapeReplaceNoEncode: /[<>"']|&(?!(#\d{1,7}|#[Xx][a-fA-F0-9]{1,6}|\w+);)/g,
    unescapeTest: /&(#(?:\d+)|(?:#x[0-9A-Fa-f]+)|(?:\w+));?/ig,
    caret: /(^|[^\[])\^/g,
    percentDecode: /%25/g,
    findPipe: /\|/g,
    splitPipe: / \|/,
    slashPipe: /\\\|/g,
    carriageReturn: /\r\n|\r/g,
    spaceLine: /^ +$/gm,
    notSpaceStart: /^\S*/,
    endingNewline: /\n$/,
    listItemRegex: (bull) => new RegExp(`^( {0,3}${bull})((?:[\t ][^\\n]*)?(?:\\n|$))`),
    nextBulletRegex: (indent) => new RegExp(`^ {0,${Math.min(3, indent - 1)}}(?:[*+-]|\\d{1,9}[.)])((?:[ \t][^\\n]*)?(?:\\n|$))`),
    hrRegex: (indent) => new RegExp(`^ {0,${Math.min(3, indent - 1)}}((?:- *){3,}|(?:_ *){3,}|(?:\\* *){3,})(?:\\n+|$)`),
    fencesBeginRegex: (indent) => new RegExp(`^ {0,${Math.min(3, indent - 1)}}(?:\`\`\`|~~~)`),
    headingBeginRegex: (indent) => new RegExp(`^ {0,${Math.min(3, indent - 1)}}#`),
    htmlBeginRegex: (indent) => new RegExp(`^ {0,${Math.min(3, indent - 1)}}<(?:[a-z].*>|!--)`, 'i'),
};

function escape(html, encode) {
    if (encode) {
        if (other.escapeTest.test(html)) {
            return html.replace(other.escapeReplace, getEscapeReplacement);
        }
    } else {
        if (other.escapeTestNoEncode.test(html)) {
            return html.replace(other.escapeReplaceNoEncode, getEscapeReplacement);
        }
    }
    return html;
}

function cleanUrl(href) {
    try {
        href = encodeURI(href).replace(other.percentDecode, '%');
    } catch {
        return null;
    }
    return href;
}

const inlineRule = /^(\${1,2})(?!\$)((?:\\.|[^\\\n])*?(?:\\.|[^\\\n\$]))\1(?=[\s?!\.,:？！。，：]|$)/;
const inlineRuleNonStandard = /^(\${1,2})(?!\$)((?:\\.|[^\\\n])*?(?:\\.|[^\\\n\$]))\1/; // Non-standard, even if there are no spaces before and after $ or $$, try to parse

const blockRule = /^(\${1,2})\n((?:\\[^]|[^\\])+?)\n\1(?:\n|$)/;

export function katexExtensions(options = {}) {
    return {
        extensions: [
            inlineKatex(options),
            blockKatex(options),
        ],
    };
}

function tryMath(text) {
    return text.replace(/\"/g, '&quot;')
}

function inlineKatex(options) {
    const nonStandard = options && options.nonStandard;
    const ruleReg = nonStandard ? inlineRuleNonStandard : inlineRule;
    return {
        name: 'inlineKatex',
        level: 'inline',
        start(src) {
            let index;
            let indexSrc = src;

            while (indexSrc) {
                index = indexSrc.indexOf('$');
                if (index === -1) {
                    return;
                }
                const f = nonStandard ? index > -1 : index === 0 || indexSrc.charAt(index - 1) === ' ';
                if (f) {
                    const possibleKatex = indexSrc.substring(index);

                    if (possibleKatex.match(ruleReg)) {
                        return index;
                    }
                }

                indexSrc = indexSrc.substring(index + 1).replace(/^\$+/, '');
            }
        },
        tokenizer(src, tokens) {
            const match = src.match(ruleReg);
            if (match) {
                return {
                    type: 'inlineKatex',
                    raw: match[0],
                    text: match[2].trim(),
                    displayMode: match[1].length === 2,
                };
            }
        },
        renderer: (token) => {
            // TODO: fix this
            // return `<n-equation value="${tryMath(token.text)}" :katex-options="{displayMode: false, strict: false}"/>\n`
            return katex.renderToString(token.text, {...options, displayMode: token.displayMode})
        },
    };
}

function blockKatex(options) {
    return {
        name: 'blockKatex',
        level: 'block',
        tokenizer(src, tokens) {
            const match = src.match(blockRule);
            if (match) {
                return {
                    type: 'blockKatex',
                    raw: match[0],
                    text: match[2].trim(),
                    displayMode: match[1].length === 2,
                };
            }
        },
        renderer: (token) => {
            // TODO: fix this
            // return `<n-equation value="${tryMath(token.text)}" :katex-options="{displayMode: true, strict: false}"/>\n`
            return katex.renderToString(token.text, {...options, displayMode: token.displayMode}) + '\n'
        },
    };
}

function tryEscape(text) {
    let encode = text.replace(/\"/g, '&quot;');
    encode = encode.replace(/\'/g, '&#39;');
    encode = encode.replace(/&/g, '&amp;');
    return encode
}

function ifFirst(level) {
    return level === "1" || level === 1;
}

export const renderer = {
    space(token) {
        return '';
    },
    code({text, lang, escaped}) {
        const langString = (lang || '').match(other.notSpaceStart)?.[0];
        const code = text.replace(other.endingNewline, '') + '\n';
        let encode = tryEscape(code)
        if (!langString) {
            return '<n-code style="overflow: auto" code="'
                + (escaped ? code : escape(code, true))
                + '\'"/>\n';
        }
        return '<n-code style="overflow: auto" class="'
            + ` mt-5 " code="${escape(code, true)}" language="${escape(lang)}" :show-line-numbers="true" />`
            + '\n';
    },
    blockquote({tokens}) {
        const body = this.parser.parse(tokens);
        return `<n-blockquote>\n${body}</n-blockquote>\n`
    },
    html({text}) {
        return text;
    },
    heading({tokens, depth, text, raw}) {
        const id = this.options.headerPrefix + text;
        const prefix = ifFirst(depth) ? 'prefix="bar"' : "";
        return `<n-h${depth} id="${id}" type="primary" ${prefix}><n-text type="primary">${this.parser.parseInline(tokens)}</n-text></n-h${depth}>\n`;
    },

    hr(token) {
        return '<n-hr/>\n';
    },
    list(token) {
        const ordered = token.ordered;
        const start = token.start;
        let body = '';
        for (let j = 0; j < token.items.length; j++) {
            const item = token.items[j];
            body += this.listitem(item);
        }
        const type = ordered ? 'n-ol' : 'n-ul';
        const startAttr = (ordered && start !== 1) ? (' start="' + start + '"') : '';
        return '<' + type + startAttr + '>\n' + body + '</' + type + '>\n';
    },
    listitem(item) {
        let itemBody = '';
        if (item.task) {
            const checkbox = this.checkbox({checked: !!item.checked});
            if (item.loose) {
                if (item.tokens[0]?.type === 'paragraph') {
                    item.tokens[0].text = checkbox + ' ' + item.tokens[0].text;
                    if (item.tokens[0].tokens && item.tokens[0].tokens.length > 0 && item.tokens[0].tokens[0].type === 'text') {
                        item.tokens[0].tokens[0].text = checkbox + ' ' + escape(item.tokens[0].tokens[0].text);
                        item.tokens[0].tokens[0].escaped = true;
                    }
                } else {
                    item.tokens.unshift({
                        type: 'text',
                        raw: checkbox + ' ',
                        text: checkbox + ' ',
                        escaped: true,
                    });
                }
            } else {
                itemBody += checkbox + ' ';
            }
        }
        itemBody += this.parser.parse(item.tokens, !!item.loose);
        return `<n-li>${itemBody}</n-li>\n`;
    },
    checkbox({checked}) {
        return '<n-checkbox class="mr-2 ml-2" '
            + (checked ? 'checked' : '')
            + ' disabled'
            + '/>';
    },
    paragraph({tokens, text}) {
        return `<p>${this.parser.parseInline(tokens)}</p>\n`;
    },
    table(token) {
        let header = '';
        // header
        let cell = '';
        for (let j = 0; j < token.header.length; j++) {
            cell += this.tablecell(token.header[j]);
        }
        header += this.tablerow({text: cell});
        let body = '';
        for (let j = 0; j < token.rows.length; j++) {
            const row = token.rows[j];
            cell = '';
            for (let k = 0; k < row.length; k++) {
                cell += this.tablecell(row[k]);
            }
            body += this.tablerow({text: cell});
        }
        if (body)
            body = `<tbody>${body}</tbody>`;
        return '<n-table size="small" :single-line="false" class="w-auto" style="overflow: auto">\n'
            + '<thead>\n'
            + header
            + '</thead>\n'
            + body
            + '</n-table>\n';
    },
    tablerow({text}) {
        return `<tr>\n${text}</tr>\n`;
    },
    tablecell(token) {
        const content = this.parser.parseInline(token.tokens);
        const type = token.header ? 'th' : 'td';
        const tag = token.align
            ? `<${type} align="${token.align}">`
            : `<${type}>`;
        return tag + content + `</${type}>\n`;
    },
    /**
     * span level renderer
     */
    strong({tokens}) {
        return `<span class="font-bold">${this.parser.parseInline(tokens)}</span>`;
    },
    em({tokens}) {
        return `<span class="italic">${this.parser.parseInline(tokens)}</span>`;
    },
    codespan({text}) {
        return `<n-text code>${escape(text, true)}</n-text>`;
    },
    br(token) {
        return '<br/>';
    },
    del({tokens}) {
        return `<span class="line-through">${this.parser.parseInline(tokens)}</span>`;
    },
    link({href, title, tokens}) {
        const text = this.parser.parseInline(tokens);
        const cleanHref = cleanUrl(href);
        if (cleanHref === null) {
            return text;
        }
        href = cleanHref;
        let out = '<n-a href="' + href + '"';
        if (title) {
            out += ' title="' + (escape(title)) + '"';
        }
        out += ' target="_blank">' + text + '</n-a>';
        return out;
    },
    image({href, title, text}) {
        const cleanHref = cleanUrl(href);
        if (cleanHref === null) {
            return escape(text);
        }
        href = cleanHref;
        let out = `<n-image src="${href}" alt="${text}"`;
        if (title) {
            out += ` title="${escape(title)}"`;
        }
        out += '/>';
        return out;
    },
    text(token) {
        return 'tokens' in token && token.tokens
            ? this.parser.parseInline(token.tokens)
            : ('escaped' in token && token.escaped ? token.text : escape(token.text));
    }
}