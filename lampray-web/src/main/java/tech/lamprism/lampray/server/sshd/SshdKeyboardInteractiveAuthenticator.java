/*
 * Copyright (C) 2023 RollW
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

package tech.lamprism.lampray.server.sshd;

import org.apache.sshd.server.auth.keyboard.DefaultKeyboardInteractiveAuthenticator;
import org.apache.sshd.server.session.ServerSession;

/**
 * @author RollW
 */
public class SshdKeyboardInteractiveAuthenticator extends DefaultKeyboardInteractiveAuthenticator {
    @Override
    protected String getInteractionName(ServerSession session) {
        return "";
    }

    @Override
    protected String getInteractionInstruction(ServerSession session) {
        return "";
    }

    @Override
    protected String getInteractionPrompt(ServerSession session) {
        return "Password: ";
    }

    @Override
    protected String getInteractionLanguage(ServerSession session) {
        return "en";
    }

    @Override
    protected boolean isInteractionPromptEchoEnabled(ServerSession session) {
        return super.isInteractionPromptEchoEnabled(session);
    }
}
