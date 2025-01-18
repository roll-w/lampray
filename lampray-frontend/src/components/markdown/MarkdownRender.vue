<!--
  - Copyright (C) 2023 RollW
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
import {NaiveUIRenderer} from "@/markdown/render";
import {compile, h, ref, watch} from "vue";
import {marked} from "marked";
import api from "@/request/api";

export default {
    props: {
        value: {
            type: String,
            default: ""
        }
    },
    name: "MarkdownRender",
    setup(props) {
        const render = new NaiveUIRenderer()
        const val = ref('')
        const renderMarkdown = (value) => {

            const renderedContent = marked(value.replace(/\\/g, '\\\\'), {
                renderer: render,
                gfm: true,
                baseUrl: api.frontBaseUrl(),
                pedantic: false,
                sanitize: false,
                tables: true,
                breaks: false,
                smartLists: true,
                smartypants: false,
                xhtml: false
            })
            if (renderedContent instanceof Promise) {
                renderedContent.then((content) => {
                    val.value = content
                })
            } else {
                val.value = renderedContent
            }
        }

        watch(() => props.value, () => {
            renderMarkdown(props.value)
        })

        renderMarkdown(props.value || '')
        return () => h(compile(val.value))
    }
}
</script>
