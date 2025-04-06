<!--
  - Copyright (C) 2023-2025 RollW
  -
  - Licensed under the Apache License, Version 2.0 (the "License");
  - you may not use this file except in compliance with the License.
  - You may obtain a copy of the License at
  -
  -        http://www.apache.org/licenses/LICENSE-2.0
  -
  - Unless required by applicable law or agreed to in writing, software
  - distributed under the License is distributed on an "AS IS" BASIS,
  - WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  - See the License for the specific language governing permissions and
  - limitations under the License.
  -->

<script>
import {compile, h, ref, watch} from "vue";
import {Marked} from "marked";
import {katexExtensions, renderer} from "@/markdown/renderer.js";

export default {
    props: {
        value: {
            type: String,
            default: ""
        }
    },
    name: "MarkdownRender",
    setup(props) {
        const marked = new Marked({
            async: true,
            renderer: renderer,
        })
        marked.use(katexExtensions({
            throwOnError: false,
            strict: false
        }))

        const renderedValue = ref('')
        const renderMarkdown = (value) => {
            const renderedContent = marked.parse(value.replace(/\\/g, '\\\\'))
            if (renderedContent instanceof Promise) {
                renderedContent.then((content) => {
                    renderedValue.value = content
                })
            } else {
                renderedValue.value = renderedContent
            }
        }

        watch(() => props.value, () => {
            renderMarkdown(props.value)
        })

        renderMarkdown(props.value || '')
        return () => h(compile(renderedValue.value))
    }
}
</script>
