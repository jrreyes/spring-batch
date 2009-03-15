/*
 * Copyright 2006-2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.core.configuration.xml;

import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;

/**
 * Internal parser for the &lt;step/&gt; elements inside a job. A step element
 * references a bean definition for a
 * {@link org.springframework.batch.core.Step} and goes on to (optionally) list
 * a set of transitions from that step to others with &lt;next on="pattern"
 * to="stepName"/&gt;. Used by the {@link JobParser}.
 * 
 * @see JobParser
 * 
 * @author Dave Syer
 * @author Thomas Risberg
 * @since 2.0
 */
public class InlineStepParser extends AbstractStepParser {

	/**
	 * Parse the step and turn it into a list of transitions.
	 * 
	 * @param element the &lt;step/gt; element to parse
	 * @param parserContext the parser context for the bean factory
	 * @param jobRepositoryRef the reference to the jobRepository from the
	 *            enclosing tag
	 * @return a collection of bean definitions for
	 *         {@link org.springframework.batch.core.job.flow.support.StateTransition}
	 *         instances objects
	 */
	public Collection<BeanDefinition> parse(Element element, ParserContext parserContext, String jobRepositoryRef) {

		BeanDefinitionBuilder stateBuilder = BeanDefinitionBuilder
				.genericBeanDefinition("org.springframework.batch.core.job.flow.support.state.StepState");
		String stepId = element.getAttribute("id");
		String stepRef = element.getAttribute("ref");
		String taskletRef = element.getAttribute("tasklet");

		@SuppressWarnings("unchecked")
		List<Element> listOfTaskElements = (List<Element>) DomUtils.getChildElementsByTagName(element, "tasklet");
		@SuppressWarnings("unchecked")
		List<Element> listOfListenersElements = (List<Element>) DomUtils
				.getChildElementsByTagName(element, "listeners");

		if (StringUtils.hasText(stepRef)) {
			if (StringUtils.hasText(taskletRef)) {
				parserContext.getReaderContext().error(
						"The 'tasklet' attribute can't be combined with the 'ref=\"" + stepRef
								+ "\"' attribute specification for <" + element.getNodeName() + ">", element);
			}
			if (listOfTaskElements.size() > 0) {
				parserContext.getReaderContext().error(
						"The <" + listOfTaskElements.get(0).getNodeName()
								+ "> element can't be combined with the 'ref=\"" + stepRef
								+ "\"' attribute specification for <" + element.getNodeName() + ">", element);
			}
			if (listOfListenersElements.size() > 0) {
				parserContext.getReaderContext().error(
						"The 'listeners' element can't be combined with the 'ref=\"" + stepRef
								+ "\"' attribute specification for <" + element.getNodeName() + ">", element);
			}
			if (StringUtils.hasText(element.getAttribute("parent"))) {
				parserContext.getReaderContext().error(
						"The 'parent' element can't be combined with the 'ref=\"" + stepRef
								+ "\"' attribute specification for <" + element.getNodeName() + ">", element);
			}
			BeanDefinitionBuilder stepBuilder = BeanDefinitionBuilder
					.genericBeanDefinition("org.springframework.batch.core.configuration.xml.DelegatingStep");
			stepBuilder.addConstructorArgValue(stepId);
			stepBuilder.addConstructorArgReference(stepRef);
			AbstractBeanDefinition bd = stepBuilder.getBeanDefinition();
			bd.setSource(parserContext.extractSource(element));
			parserContext.getRegistry().registerBeanDefinition(stepId, bd);
			stateBuilder.addConstructorArgReference(stepId);
		}
		else {
			AbstractBeanDefinition bd = parseTasklet(element, parserContext, jobRepositoryRef);
			if (bd != null) {
				parserContext.registerBeanComponent(new BeanComponentDefinition(bd, stepId));
				stateBuilder.addConstructorArgReference(stepId);
			}
		}
		return FlowParser.getNextElements(parserContext, stepId, stateBuilder.getBeanDefinition(), element);

	}

}