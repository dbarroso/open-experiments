/*
 * Licensed to the Sakai Foundation (SF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.sakaiproject.nakamura.files.servlets;

import static org.apache.sling.jcr.resource.JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.commons.testing.jcr.MockNode;
import org.apache.sling.commons.testing.jcr.MockNodeIterator;
import org.apache.sling.commons.testing.jcr.MockProperty;
import org.apache.sling.commons.testing.jcr.MockPropertyIterator;
import org.junit.Test;
import org.sakaiproject.nakamura.api.files.FilesConstants;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.servlet.ServletException;

/**
 *
 */
public class TagServletTest {

  @Test
  public void testChildren() throws RepositoryException, IOException, ServletException,
      JSONException {

    // We create a tree of nodes and take one out of the middle.
    Node tag = createTagTree();
    SlingHttpServletRequest request = mock(SlingHttpServletRequest.class);
    RequestPathInfo pathInfo = mock(RequestPathInfo.class);
    when(pathInfo.getSelectorString()).thenReturn("children");
    when(request.getRequestPathInfo()).thenReturn(pathInfo);
    Resource resource = mock(Resource.class);
    when(resource.adaptTo(Node.class)).thenReturn(tag);
    when(request.getResource()).thenReturn(resource);

    SlingHttpServletResponse response = mock(SlingHttpServletResponse.class);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintWriter w = new PrintWriter(baos);
    when(response.getWriter()).thenReturn(w);

    TagServlet servlet = new TagServlet();
    servlet.doGet(request, response);
    w.flush();

    String s = baos.toString("UTF-8");
    JSONObject j = new JSONObject(s);

    assertEquals("tagB", j.getString("jcr:name"));
    assertEquals("/tags/tagA/tagB", j.getString("jcr:path"));
    JSONArray tagBChildren = j.getJSONArray("children");
    assertEquals(2, tagBChildren.length());
    assertEquals("tagC", tagBChildren.getJSONObject(0).getString("jcr:name"));
    assertEquals("/tags/tagA/tagB/tagC", tagBChildren.getJSONObject(0).getString(
        "jcr:path"));
    assertEquals("tagD", tagBChildren.getJSONObject(1).getString("jcr:name"));
    assertEquals("/tags/tagA/tagB/tagD", tagBChildren.getJSONObject(1).getString(
        "jcr:path"));
  }

  @Test
  public void testParents() throws JSONException, RepositoryException, ServletException,
      IOException {
    // We create a tree of nodes and take one out of the middle.
    Node tag = createTagTree();
    SlingHttpServletRequest request = mock(SlingHttpServletRequest.class);
    RequestPathInfo pathInfo = mock(RequestPathInfo.class);
    when(pathInfo.getSelectorString()).thenReturn("parents");
    when(request.getRequestPathInfo()).thenReturn(pathInfo);
    Resource resource = mock(Resource.class);
    when(resource.adaptTo(Node.class)).thenReturn(tag);
    when(request.getResource()).thenReturn(resource);

    SlingHttpServletResponse response = mock(SlingHttpServletResponse.class);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintWriter w = new PrintWriter(baos);
    when(response.getWriter()).thenReturn(w);

    TagServlet servlet = new TagServlet();
    servlet.doGet(request, response);
    w.flush();

    String s = baos.toString("UTF-8");
    JSONObject j = new JSONObject(s);

    assertEquals("tagB", j.getString("jcr:name"));
    assertEquals("/tags/tagA/tagB", j.getString("jcr:path"));
    JSONObject parent = j.getJSONObject("parent");
    assertEquals("tagA", parent.getString("jcr:name"));
    assertEquals("/tags/tagA", parent.getString("jcr:path"));
  }

  /**
   * Creates a tree of tag nodes. tag A + tag B + tag C + random file + tag D
   * 
   * @return tag B
   */
  public Node createTagTree() throws RepositoryException {
    Node tagA = createTag("tagA", "/tags/tagA");
    Node tagB = createTag("tagB", "/tags/tagA/tagB");
    Node tagC = createTag("tagC", "/tags/tagA/tagB/tagC");
    Node tagD = createTag("tagD", "/tags/tagA/tagB/tagD");
    Node randomNode = new MockNode("/tags/tagA/tagB/randomNode");

    when(tagA.getNodes()).thenReturn(new MockNodeIterator(new Node[] { tagB }));
    when(tagB.getNodes()).thenReturn(
        new MockNodeIterator(new Node[] { tagC, tagD, randomNode }));
    when(tagC.getNodes()).thenReturn(new MockNodeIterator());
    when(tagD.getNodes()).thenReturn(new MockNodeIterator());

    when(tagD.getParent()).thenReturn(tagB);
    when(tagC.getParent()).thenReturn(tagB);
    when(tagB.getParent()).thenReturn(tagA);
    when(tagA.getParent()).thenReturn(new MockNode("/tags"));

    return tagB;

  }

  private Node createTag(String name, String path) throws RepositoryException {
    Node tag = mock(Node.class);
    Property rtProp = new MockProperty(SLING_RESOURCE_TYPE_PROPERTY);
    rtProp.setValue(FilesConstants.RT_SAKAI_TAG);
    Property tagNameProp = new MockProperty(FilesConstants.SAKAI_TAG_NAME);
    tagNameProp.setValue(name);

    when(tag.getProperty(SLING_RESOURCE_TYPE_PROPERTY)).thenReturn(rtProp);
    when(tag.hasProperty(SLING_RESOURCE_TYPE_PROPERTY)).thenReturn(true);
    when(tag.getName()).thenReturn(name);
    when(tag.getPath()).thenReturn(path);

    List<Property> props = new ArrayList<Property>();
    props.add(rtProp);
    props.add(tagNameProp);
    MockPropertyIterator iterator = new MockPropertyIterator(props.iterator());
    when(tag.getProperties()).thenReturn(iterator);
    return tag;
  }

}
