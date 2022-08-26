/* 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.esigate.esi;

import java.io.IOException;

import org.esigate.HttpErrorPage;
import org.esigate.parser.ElementType;
import org.esigate.parser.ParserContext;

class ExceptElement extends BaseElement {

    public static final ElementType TYPE = new BaseElementType("<esi:except", "</esi:except") {
        @Override
        public ExceptElement newInstance() {
            return new ExceptElement();
        }

    };

    private boolean processContent;
    private boolean shouldThrowError;

    ExceptElement() {
    }

    @Override
    protected boolean parseTag(Tag tag, ParserContext ctx) {
        TryElement parent = ctx.findAncestor(TryElement.class);
        int code = -1;
        int forcedResponseCode = -1;
        if (tag.getAttribute("code") != null) {
            code = Integer.parseInt(tag.getAttribute("code"));
        }
        if (tag.getAttribute("responseCode") != null) {
            forcedResponseCode = Integer.parseInt(tag.getAttribute("responseCode"));
        }

        processContent =
                (parent.hasErrors() && !parent.exceptProcessed() && (code == -1 || code == parent.getErrorCode()));
        shouldThrowError = false;
        if (processContent) {
            parent.setExceptProcessed(true);

            if (forcedResponseCode != -1 && parent.hasHttpError()) {
                parent.getHttpError().getHttpResponse().setStatusCode(forcedResponseCode);
                shouldThrowError = true;
            }
        }
        return processContent;
    }

    @Override
    public boolean onTagStart(String tag, ParserContext ctx) throws IOException, HttpErrorPage {
        TryElement parent = ctx.findAncestor(TryElement.class);
        parent.setWrite(true);
        return super.onTagStart(tag, ctx);
    }

    @Override
    public void onTagEnd(String tag, ParserContext ctx) throws HttpErrorPage {
        TryElement parent = ctx.findAncestor(TryElement.class);
        parent.setWrite(false);
		if(this.processContent && this.shouldThrowError) {
			throw parent.getHttpError();
		}
    }

    @Override
    public void characters(CharSequence csq, int start, int end) throws IOException {
        if (this.processContent) {
            super.characters(csq, start, end);
        }
    }

}
