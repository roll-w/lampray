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

<script setup>
import {usePage} from "@/views/utils/pages";
import {useRouter} from "vue-router";
import {ref} from "vue";
import {NPagination} from "naive-ui";

const props = defineProps({
    page: {
        /**
         * @type {{
         * page: number,
         * count: number
         * }}
         */
        type: Object,
        default: null
    },
    routeName: {
        type: String,
        required: true
    }
})

const router = useRouter();
const currentRoute = router.currentRoute;

const currentParams = currentRoute.value.params;
const currentQuery = currentRoute.value.query;

const page = props.page ? ref(props.page) : usePage();

const getRouteOption = (page) => {
    return {
        name: props.routeName,
        params: currentParams,
        query: {
            ...currentQuery,
            page: page
        }
    }
}

const onSwitchPage = (page) => {
    router.push(getRouteOption(page))
}
</script>

<template>
    <div class="flex items-start justify-start mt-5">
        <div>
            <n-pagination
                    v-model:page="page.page"
                    :on-update-page="onSwitchPage"
                    :page-count="page.count"
                    show-quick-jumper
            />
        </div>
    </div>
</template>
