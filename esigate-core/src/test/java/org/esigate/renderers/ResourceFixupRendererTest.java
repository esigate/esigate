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

package org.esigate.renderers;

import java.io.IOException;
import java.io.Writer;

import junit.framework.TestCase;

import org.apache.commons.io.output.StringBuilderWriter;
import org.esigate.impl.UrlRewriter;
import org.mockito.Mockito;

/**
 * Tests on ResourceFixupRenderer.
 * 
 * @author Nicolas Richeton
 * 
 */
public class ResourceFixupRendererTest extends TestCase {

    public void testRenderBlock1() throws IOException {
        String baseUrl = "http://backend/context";
        String requestUrl = "path/page.html";
        final String input = "some html";

        UrlRewriter urlRewriter = Mockito.mock(UrlRewriter.class);
        Mockito.when(urlRewriter.rewriteHtml(input, requestUrl, baseUrl)).thenReturn("url rewritten html");

        Writer out = new StringBuilderWriter();
        ResourceFixupRenderer tested = new ResourceFixupRenderer(baseUrl, requestUrl, urlRewriter);
        tested.render(null, input, out);

        Mockito.verify(urlRewriter, Mockito.times(1)).rewriteHtml(input, requestUrl, baseUrl);
        assertEquals("url rewritten html", out.toString());
    }
}
