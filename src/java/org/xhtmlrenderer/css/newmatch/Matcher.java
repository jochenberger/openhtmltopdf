/*
 * Matcher.java
 * Copyright (c) 2004, 2005 Torbj�rn Gannholm
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.	See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 */
package org.xhtmlrenderer.css.newmatch;

import org.xhtmlrenderer.css.sheet.Stylesheet;
import org.xhtmlrenderer.css.sheet.StylesheetFactory;
import org.xhtmlrenderer.css.sheet.StylesheetInfo;
import org.xhtmlrenderer.util.XRLog;

import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;


/**
 * @author Torbj�rn Gannholm
 */
public class Matcher {

    /**
     * Description of the Field
     */
    Mapper docMapper;
    /**
     * Description of the Field
     */
    private org.xhtmlrenderer.css.newmatch.AttributeResolver _attRes;
    /**
     * Description of the Field
     */
    private org.xhtmlrenderer.css.sheet.StylesheetFactory _styleFactory;

    /**
     * Description of the Field
     */
    private java.util.HashMap _map;
    /**
     * Description of the Field
     */
    private java.util.HashMap _peMap;
    /**
     * Description of the Field
     */
    private java.util.HashMap _csCache;
    //private java.util.HashMap _elStyle;

    //handle dynamic
    /**
     * Description of the Field
     */
    private Set _hoverElements;
    /**
     * Description of the Field
     */
    private Set _activeElements;
    /**
     * Description of the Field
     */
    private Set _focusElements;
    /**
     * Description of the Field
     */
    private Set _visitElements;

    /**
     * creates a new matcher for the combination of parameters
     *
     * @param ar          PARAM
     * @param factory     PARAM
     * @param stylesheets PARAM
     * @param media       PARAM
     */
    public Matcher(AttributeResolver ar, StylesheetFactory factory, Iterator stylesheets, String media) {
        newMaps();
        _attRes = ar;
        _styleFactory = factory;
        docMapper = createDocumentMapper(stylesheets, media);
    }

    /**
     * Gets the cascadedStyle attribute of the Matcher object
     *
     * @param e       PARAM
     * @param restyle PARAM
     * @return The cascadedStyle value
     */
    public CascadedStyle getCascadedStyle(org.w3c.dom.Element e, boolean restyle) {
        Mapper em;
        if (!restyle) {
            em = getMapper(e);
        } else {
            em = matchElement(e);
        }
        return em.style;
    }

    /**
     * May return null
     *
     * @param e             PARAM
     * @param pseudoElement PARAM
     * @return The pECascadedStyle value
     */
    public CascadedStyle getPECascadedStyle(org.w3c.dom.Element e, String pseudoElement) {
        java.util.Map elm = (java.util.Map) _peMap.get(e);
        if (elm == null) {
            return null;
        }
        return (CascadedStyle) elm.get(pseudoElement);
    }

    /**
     * Gets the visitedStyled attribute of the Matcher object
     *
     * @param e PARAM
     * @return The visitedStyled value
     */
    public boolean isVisitedStyled(org.w3c.dom.Element e) {
        return _visitElements.contains(e);
    }

    /**
     * Gets the hoverStyled attribute of the Matcher object
     *
     * @param e PARAM
     * @return The hoverStyled value
     */
    public boolean isHoverStyled(org.w3c.dom.Element e) {
        return _hoverElements.contains(e);
    }

    /**
     * Gets the activeStyled attribute of the Matcher object
     *
     * @param e PARAM
     * @return The activeStyled value
     */
    public boolean isActiveStyled(org.w3c.dom.Element e) {
        return _activeElements.contains(e);
    }

    /**
     * Gets the focusStyled attribute of the Matcher object
     *
     * @param e PARAM
     * @return The focusStyled value
     */
    public boolean isFocusStyled(org.w3c.dom.Element e) {
        return _focusElements.contains(e);
    }

    /**
     * Description of the Method
     *
     * @param e PARAM
     * @return Returns
     */
    protected Mapper matchElement(org.w3c.dom.Element e) {
        org.w3c.dom.Node parent = e.getParentNode();
        Mapper child;
        if (parent.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
            Mapper m = getMapper((org.w3c.dom.Element) parent);
            child = m.mapChild(e);
        } else {//has to be document or fragment node
            child = docMapper.mapChild(e);
        }
        return child;
    }

    /**
     * Description of the Method
     *
     * @param stylesheets PARAM
     * @param media       PARAM
     * @return Returns
     */
    Mapper createDocumentMapper(java.util.Iterator stylesheets, String media) {
        int count = 0;
        java.util.TreeMap sorter = new java.util.TreeMap();
        while (stylesheets.hasNext()) {
            count = appendStylesheet((StylesheetInfo) stylesheets.next(), count, sorter, media);
        }
        XRLog.match("Matcher created with " + sorter.size() + " selectors");
        return new Mapper(sorter.values());
    }

    /**
     * Description of the Method
     *
     * @param e                PARAM
     * @param matchedSelectors PARAM
     * @return Returns
     */
    CascadedStyle createCascadedStyle(org.w3c.dom.Element e, List matchedSelectors) {
        CascadedStyle cs = null;
        org.xhtmlrenderer.css.sheet.Ruleset elementStyling = getElementStyle(e);
        java.util.List propList = new java.util.LinkedList();
        for (java.util.Iterator i = getMatchedRulesets(matchedSelectors); i.hasNext();) {
            org.xhtmlrenderer.css.sheet.Ruleset rs = (org.xhtmlrenderer.css.sheet.Ruleset) i.next();
            for (java.util.Iterator j = rs.getPropertyDeclarations(); j.hasNext();) {
                propList.add((org.xhtmlrenderer.css.sheet.PropertyDeclaration) j.next());
            }
        }
        if (elementStyling != null) {
            for (java.util.Iterator j = elementStyling.getPropertyDeclarations(); j.hasNext();) {
                propList.add((org.xhtmlrenderer.css.sheet.PropertyDeclaration) j.next());
            }
        }
        if (propList.size() == 0)
            cs = CascadedStyle.emptyCascadedStyle;//already internalized
        else {
            cs = new CascadedStyle(propList.iterator());
            //internalize (worth the trouble to make style-caching work)
            String fingerprint = cs.getFingerprint();
            CascadedStyle internal = (CascadedStyle) _csCache.get(fingerprint);
            if (internal != null) {
                cs = internal;
            } else {
                _csCache.put(fingerprint, cs);
            }
        }

        return cs;
    }

    /**
     * Description of the Method
     *
     * @param e PARAM
     * @param m PARAM
     */
    private void link(org.w3c.dom.Element e, Mapper m) {
        _map.put(e, new SoftReference(m));
    }

    /**
     * Description of the Method
     */
    private void newMaps() {
        _map = new java.util.HashMap();
        _csCache = new java.util.HashMap();
        _peMap = new java.util.HashMap();
        //_elStyle = new java.util.HashMap();
        _hoverElements = new java.util.HashSet();
        _activeElements = new java.util.HashSet();
        _focusElements = new java.util.HashSet();
        _visitElements = new java.util.HashSet();
    }

    /**
     * Description of the Method
     *
     * @param si     PARAM
     * @param count  PARAM
     * @param sorter PARAM
     * @param media  PARAM
     * @return Returns
     */
    private int appendStylesheet(StylesheetInfo si, int count, java.util.TreeMap sorter, String media) {
        if (!si.appliesToMedia(media)) {
            return count;
        }//this is logical, and also how firefox does it
        Stylesheet ss = si.getStylesheet();
        if (ss == null) {
            ss = _styleFactory.getStylesheet(si);
            si.setStylesheet(ss);
        }
        if (ss == null) {
            return count;
        }//couldn't load it
        for (java.util.Iterator rulesets = ss.getRulesets(); rulesets.hasNext();) {
            Object obj = rulesets.next();
            if (obj instanceof StylesheetInfo) {
                count = appendStylesheet((StylesheetInfo) obj, count, sorter, media);
            } else {
                org.xhtmlrenderer.css.sheet.Ruleset r = (org.xhtmlrenderer.css.sheet.Ruleset) obj;
                //at this point all selectors in a ruleset must be placed on the descendant axis
                org.w3c.css.sac.SelectorList selector_list = r.getSelectorList();
                for (int i = 0; i < selector_list.getLength(); i++) {
                    org.w3c.css.sac.Selector selector = selector_list.item(i);
                    Selector s = addSelector(count++, r, selector);
                    sorter.put(s.getOrder(), s);
                }
            }
        }
        return count;
    }

    /**
     * Turn the selection logic inside-out: we want to start as close to the
     * root element as possible, while sac starts at the final selected element
     *
     * @param pos      The feature to be added to the Selector attribute
     * @param rs       The feature to be added to the ChainedSelector attribute
     * @param selector The feature to be added to the ChainedSelector attribute
     * @return Returns
     */
    private Selector addSelector(int pos, org.xhtmlrenderer.css.sheet.Ruleset rs, org.w3c.css.sac.Selector selector) {
        Selector s = null;
        if (selector.getSelectorType() == org.w3c.css.sac.Selector.SAC_DIRECT_ADJACENT_SELECTOR) {
            s = addSiblingSelector(pos, rs, selector);
        } else if (selector.getSelectorType() == org.w3c.css.sac.Selector.SAC_CHILD_SELECTOR) {
            s = addSelector(pos, rs, ((org.w3c.css.sac.DescendantSelector) selector).getAncestorSelector());
            addChainedSelector(s, selector);
        } else if (selector.getSelectorType() == org.w3c.css.sac.Selector.SAC_DESCENDANT_SELECTOR) {
            s = addSelector(pos, rs, ((org.w3c.css.sac.DescendantSelector) selector).getAncestorSelector());
            addChainedSelector(s, selector);
        } else if (selector.getSelectorType() == org.w3c.css.sac.Selector.SAC_CONDITIONAL_SELECTOR) {
            org.w3c.css.sac.Condition cond = ((org.w3c.css.sac.ConditionalSelector) selector).getCondition();
            s = addSelector(pos, rs, ((org.w3c.css.sac.ConditionalSelector) selector).getSimpleSelector());
            addConditions(s, cond);
        } else if (selector.getSelectorType() == org.w3c.css.sac.Selector.SAC_ELEMENT_NODE_SELECTOR) {
            s = new Selector(pos, rs, Selector.DESCENDANT_AXIS, ((org.w3c.css.sac.ElementSelector) selector).getLocalName());
        } else {
            XRLog.exception("unsupported selector in addSelector: " + selector.getSelectorType());
        }

        return s;
    }

    /**
     * @param s        The feature to be added to the ChainedSelector attribute
     * @param selector The feature to be added to the ChainedSelector attribute
     */
    private void addChainedSelector(Selector s, org.w3c.css.sac.Selector selector) {
        int axis = 0;
        org.w3c.css.sac.SimpleSelector simple = null;
        switch (selector.getSelectorType()) {
            case org.w3c.css.sac.Selector.SAC_CHILD_SELECTOR:
                axis = Selector.CHILD_AXIS;
                simple = ((org.w3c.css.sac.DescendantSelector) selector).getSimpleSelector();
                break;
            case org.w3c.css.sac.Selector.SAC_DESCENDANT_SELECTOR:
                axis = Selector.DESCENDANT_AXIS;
                simple = ((org.w3c.css.sac.DescendantSelector) selector).getSimpleSelector();
                break;
            default:
                System.err.println("Bad selector in addChainedSelector");
        }

        org.w3c.css.sac.Condition cond = null;
        if (simple.getSelectorType() == org.w3c.css.sac.Selector.SAC_CONDITIONAL_SELECTOR) {
            cond = ((org.w3c.css.sac.ConditionalSelector) simple).getCondition();
            //if ConditionalSelectors can be nested, we are in trouble here
            simple = ((org.w3c.css.sac.ConditionalSelector) simple).getSimpleSelector();
        }
        if (simple.getSelectorType() == org.w3c.css.sac.Selector.SAC_ELEMENT_NODE_SELECTOR) {
            s = s.appendChainedSelector(axis, ((org.w3c.css.sac.ElementSelector) simple).getLocalName());
        }
        if (cond != null) {
            addConditions(s, cond);
        }
    }

    /**
     * @param pos             The feature to be added to the SiblingSelector
     *                        attribute
     * @param rs              The feature to be added to the SiblingSelector
     *                        attribute
     * @param siblingSelector The feature to be added to the ChainedSelector
     *                        attribute
     * @return Returns
     */
    private Selector addSiblingSelector(int pos, org.xhtmlrenderer.css.sheet.Ruleset rs, org.w3c.css.sac.Selector siblingSelector) {
        int axis = Selector.DESCENDANT_AXIS;
        org.w3c.css.sac.SimpleSelector simple = null;
        org.w3c.css.sac.Selector sib = null;
        switch (siblingSelector.getSelectorType()) {
            case org.w3c.css.sac.Selector.SAC_DIRECT_ADJACENT_SELECTOR:
                simple = ((org.w3c.css.sac.SiblingSelector) siblingSelector).getSiblingSelector();
                sib = ((org.w3c.css.sac.SiblingSelector) siblingSelector).getSelector();
                break;
            default:
                XRLog.exception("Bad selector in addSiblingSelector");
        }

        Selector ancestor = null;
        Selector current = null;
        org.w3c.css.sac.Selector pa = sib;
        while (pa.getSelectorType() == org.w3c.css.sac.Selector.SAC_DIRECT_ADJACENT_SELECTOR) {
            pa = ((org.w3c.css.sac.SiblingSelector) pa).getSelector();
        }
        if (pa.getSelectorType() == org.w3c.css.sac.Selector.SAC_CHILD_SELECTOR) {
            ancestor = addSelector(pos, rs, ((org.w3c.css.sac.DescendantSelector) pa).getAncestorSelector());
            axis = Selector.CHILD_AXIS;
        } else if (pa.getSelectorType() == org.w3c.css.sac.Selector.SAC_DESCENDANT_SELECTOR) {
            ancestor = addSelector(pos, rs, ((org.w3c.css.sac.DescendantSelector) pa).getAncestorSelector());
        } else if (pa.getSelectorType() == org.w3c.css.sac.Selector.SAC_CONDITIONAL_SELECTOR) {
            //do nothing, no ancestor selector exists
        } else if (pa.getSelectorType() == org.w3c.css.sac.Selector.SAC_ELEMENT_NODE_SELECTOR) {
            //do nothing, no ancestor selector exists
        } else {
            XRLog.exception("unsupported selector in addSiblingSelector: " + pa.getSelectorType());
        }

        org.w3c.css.sac.Condition cond = null;
        if (simple.getSelectorType() == org.w3c.css.sac.Selector.SAC_CONDITIONAL_SELECTOR) {
            cond = ((org.w3c.css.sac.ConditionalSelector) simple).getCondition();
            //if ConditionalSelectors can be nested, we are in trouble here
            simple = ((org.w3c.css.sac.ConditionalSelector) simple).getSimpleSelector();
        }
        if (simple.getSelectorType() == org.w3c.css.sac.Selector.SAC_ELEMENT_NODE_SELECTOR) {
            if (ancestor == null) {
                current = new Selector(pos, rs, Selector.DESCENDANT_AXIS, ((org.w3c.css.sac.ElementSelector) simple).getLocalName());
                ancestor = current;
            } else {
                current = ancestor.appendChainedSelector(axis, ((org.w3c.css.sac.ElementSelector) simple).getLocalName());
            }
        }
        if (cond != null) {
            addConditions(current, cond);
        }

        while (sib.getSelectorType() == org.w3c.css.sac.Selector.SAC_DIRECT_ADJACENT_SELECTOR) {
            simple = ((org.w3c.css.sac.SiblingSelector) sib).getSiblingSelector();
            cond = null;
            Selector s = null;
            if (simple.getSelectorType() == org.w3c.css.sac.Selector.SAC_CONDITIONAL_SELECTOR) {
                cond = ((org.w3c.css.sac.ConditionalSelector) simple).getCondition();
                //if ConditionalSelectors can be nested, we are in trouble here
                simple = ((org.w3c.css.sac.ConditionalSelector) simple).getSimpleSelector();
            }
            if (simple.getSelectorType() == org.w3c.css.sac.Selector.SAC_ELEMENT_NODE_SELECTOR) {
                s = current.appendSiblingSelector(Selector.IMMEDIATE_SIBLING_AXIS, ((org.w3c.css.sac.ElementSelector) simple).getLocalName());
            }
            if (cond != null) {
                addConditions(s, cond);
            }
            sib = ((org.w3c.css.sac.SiblingSelector) sib).getSelector();
        }
        simple = null;
        switch (sib.getSelectorType()) {
            case org.w3c.css.sac.Selector.SAC_CHILD_SELECTOR:
                simple = ((org.w3c.css.sac.DescendantSelector) sib).getSimpleSelector();
                break;
            case org.w3c.css.sac.Selector.SAC_DESCENDANT_SELECTOR:
                simple = ((org.w3c.css.sac.DescendantSelector) sib).getSimpleSelector();
                break;
            default:
                simple = (org.w3c.css.sac.SimpleSelector) sib;
        }

        cond = null;
        Selector s = null;
        if (simple.getSelectorType() == org.w3c.css.sac.Selector.SAC_CONDITIONAL_SELECTOR) {
            cond = ((org.w3c.css.sac.ConditionalSelector) simple).getCondition();
            //if ConditionalSelectors can be nested, we are in trouble here
            simple = ((org.w3c.css.sac.ConditionalSelector) simple).getSimpleSelector();
        }
        if (simple.getSelectorType() == org.w3c.css.sac.Selector.SAC_ELEMENT_NODE_SELECTOR) {
            s = current.appendSiblingSelector(Selector.IMMEDIATE_SIBLING_AXIS, ((org.w3c.css.sac.ElementSelector) simple).getLocalName());
        }
        if (cond != null) {
            addConditions(s, cond);
        }
        return ancestor;
    }

    /**
     * @param s    The feature to be added to the Conditions attribute
     * @param cond The feature to be added to the Conditions attribute
     */
    private void addConditions(Selector s, org.w3c.css.sac.Condition cond) {
        switch (cond.getConditionType()) {
            case org.w3c.css.sac.Condition.SAC_AND_CONDITION:
                org.w3c.css.sac.CombinatorCondition comb = (org.w3c.css.sac.CombinatorCondition) cond;
                addConditions(s, comb.getFirstCondition());
                addConditions(s, comb.getSecondCondition());
                break;
            case org.w3c.css.sac.Condition.SAC_ATTRIBUTE_CONDITION:
                org.w3c.css.sac.AttributeCondition attr = (org.w3c.css.sac.AttributeCondition) cond;
                if (attr.getSpecified()) {
                    s.addAttributeEqualsCondition(attr.getLocalName(), attr.getValue());
                } else {
                    s.addAttributeExistsCondition(attr.getLocalName());
                }
                break;
            case org.w3c.css.sac.Condition.SAC_BEGIN_HYPHEN_ATTRIBUTE_CONDITION:
                attr = (org.w3c.css.sac.AttributeCondition) cond;
                s.addAttributeMatchesFirstPartCondition(attr.getLocalName(), attr.getValue());
                break;
            case org.w3c.css.sac.Condition.SAC_CLASS_CONDITION:
                attr = (org.w3c.css.sac.AttributeCondition) cond;
                s.addClassCondition(attr.getValue());
                break;
            case org.w3c.css.sac.Condition.SAC_ID_CONDITION:
                attr = (org.w3c.css.sac.AttributeCondition) cond;
                s.addIDCondition(attr.getValue());
                break;
            case org.w3c.css.sac.Condition.SAC_LANG_CONDITION:
                org.w3c.css.sac.LangCondition lang = (org.w3c.css.sac.LangCondition) cond;
                s.addLangCondition(lang.getLang());
                break;
            case org.w3c.css.sac.Condition.SAC_ONE_OF_ATTRIBUTE_CONDITION:
                attr = (org.w3c.css.sac.AttributeCondition) cond;
                s.addAttributeMatchesListCondition(attr.getLocalName(), attr.getValue());
                break;
            case org.w3c.css.sac.Condition.SAC_POSITIONAL_CONDITION:
                org.w3c.css.sac.PositionalCondition pos = (org.w3c.css.sac.PositionalCondition) cond;
                if (pos.getPosition() == 0) {
                    s.addFirstChildCondition();
                } else {
                    s.addUnsupportedCondition();
                }
                break;
            case org.w3c.css.sac.Condition.SAC_PSEUDO_CLASS_CONDITION:
                attr = (org.w3c.css.sac.AttributeCondition) cond;
                if (attr.getValue().equals("link")) {
                    s.addLinkCondition();
                } else if (attr.getValue().equals("visited")) {
                    s.setPseudoClass(Selector.VISITED_PSEUDOCLASS);
                } else if (attr.getValue().equals("hover")) {
                    s.setPseudoClass(Selector.HOVER_PSEUDOCLASS);
                } else if (attr.getValue().equals("active")) {
                    s.setPseudoClass(Selector.ACTIVE_PSEUDOCLASS);
                } else if (attr.getValue().equals("focus")) {
                    s.setPseudoClass(Selector.FOCUS_PSEUDOCLASS);
                } else {//it must be a pseudo-element
                    s.setPseudoElement(attr.getValue());
                }
                break;
            default:
                System.err.println("Bad condition");
        }
    }

    /**
     * Description of the Method
     *
     * @param e               PARAM
     * @param pseudoSelectors PARAM
     */
    private void resolvePseudoElements(org.w3c.dom.Element e, HashMap pseudoSelectors) {
        java.util.Iterator si = pseudoSelectors.entrySet().iterator();
        if (!si.hasNext()) {
            return;
        }
        java.util.Map pelm = new java.util.HashMap();
        while (si.hasNext()) {
            java.util.Map.Entry me = (java.util.Map.Entry) si.next();
            CascadedStyle cs = null;

            java.util.List propList = new java.util.LinkedList();
            for (java.util.Iterator i = getSelectedRulesets((java.util.List) me.getValue()); i.hasNext();) {
                org.xhtmlrenderer.css.sheet.Ruleset rs = (org.xhtmlrenderer.css.sheet.Ruleset) i.next();
                for (java.util.Iterator j = rs.getPropertyDeclarations(); j.hasNext();) {
                    propList.add((org.xhtmlrenderer.css.sheet.PropertyDeclaration) j.next());
                }
            }
            if (propList.size() == 0)
                cs = CascadedStyle.emptyCascadedStyle;//already internalized
            else {
                cs = new CascadedStyle(propList.iterator());
                //internalize (worth the trouble to make style-caching work)
                String fingerprint = cs.getFingerprint();
                CascadedStyle internal = (CascadedStyle) _csCache.get(fingerprint);
                if (internal != null) {
                    cs = internal;
                } else {
                    _csCache.put(fingerprint, cs);
                }
            }

            pelm.put(me.getKey(), cs);
        }
        _peMap.put(e, pelm);
    }

    /**
     * Gets the mapper attribute of the Matcher object
     *
     * @param e PARAM
     * @return The mapper value
     */
    private Mapper getMapper(org.w3c.dom.Element e) {
        if (_map == null) {
            _map = new java.util.HashMap();
        }
        Mapper m = null;
        SoftReference ref = (SoftReference) _map.get(e);
        if (ref != null) {
            m = (Mapper) ref.get();
        }
        if (m != null) {
            return m;
        }
        m = matchElement(e);
        //ref = (SoftReference) _map.get(e);
        //if (ref != null) m = (Mapper) ref.get();
        return m;
    }


    /**
     * Gets the matchedRulesets attribute of the Matcher object
     *
     * @param mappedSelectors PARAM
     * @return The matchedRulesets value
     */
    private java.util.Iterator getMatchedRulesets(final List mappedSelectors) {
        return
                new java.util.Iterator() {
                    java.util.Iterator selectors = mappedSelectors.iterator();

                    public boolean hasNext() {
                        return selectors.hasNext();
                    }

                    public Object next() {
                        if (hasNext()) {
                            return ((Selector) selectors.next()).getRuleset();
                        } else {
                            throw new java.util.NoSuchElementException();
                        }
                    }

                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
    }

    /**
     * Gets the selectedRulesets attribute of the Matcher object
     *
     * @param selectorList PARAM
     * @return The selectedRulesets value
     */
    private java.util.Iterator getSelectedRulesets(java.util.List selectorList) {
        final java.util.List sl = selectorList;
        return
                new java.util.Iterator() {
                    java.util.Iterator selectors = sl.iterator();

                    public boolean hasNext() {
                        return selectors.hasNext();
                    }

                    public Object next() {
                        if (hasNext()) {
                            return ((Selector) selectors.next()).getRuleset();
                        } else {
                            throw new java.util.NoSuchElementException();
                        }
                    }

                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
    }

    /**
     * Gets the elementStyle attribute of the Matcher object
     *
     * @param e PARAM
     * @return The elementStyle value
     */
    private org.xhtmlrenderer.css.sheet.Ruleset getElementStyle(org.w3c.dom.Element e) {
        if (_attRes == null || _styleFactory == null) {
            return null;
        }
        org.xhtmlrenderer.css.sheet.Ruleset rs;// = (org.xhtmlrenderer.css.sheet.Ruleset) _elStyle.get(e);
        //if (rs == null) {
        String style = _attRes.getElementStyling(e);
        if (style == null || style.equals("")) {
            return null;
        }
        rs = _styleFactory.parseStyleDeclaration(org.xhtmlrenderer.css.sheet.StylesheetInfo.AUTHOR, style);
        //_elStyle.put(e, rs);
        //}
        return rs;
    }

    /**
     * Mapper represents a local CSS for a Node that is used to match the Node's
     * children.
     *
     * @author Torbj�rn Gannholm
     */
    class Mapper {

        /**
         * Description of the Field
         */
        java.util.List axes = new java.util.ArrayList();
        /**
         * Description of the Field
         */
        CascadedStyle style;

        /**
         * Creates a new instance of Mapper
         *
         * @param selectors PARAM
         */
        Mapper(java.util.Collection selectors) {
            axes.addAll(selectors);
        }

        /**
         * Constructor for the Mapper object
         */
        private Mapper() {
        }

        /**
         * Side effect: creates and stores a Mapper for the element
         *
         * @param e
         * @return The selectors that matched, sorted according to specificity
         *         (more correct: preserves the sort order from Matcher creation)
         */
        Mapper mapChild(org.w3c.dom.Element e) {
            Mapper childMapper = new Mapper();
            java.util.HashMap pseudoSelectors = new java.util.HashMap();
            java.util.List mappedSelectors = new java.util.LinkedList();
            for (int i = 0; i < axes.size(); i++) {
                Selector sel = (Selector) axes.get(i);
                if (sel.getAxis() == Selector.DESCENDANT_AXIS) {
                    //carry it forward to other descendants
                    childMapper.axes.add(sel);
                } else if (sel.getAxis() == Selector.IMMEDIATE_SIBLING_AXIS) {
                    throw new RuntimeException();
                }
                if (!sel.matches(e, _attRes)) {
                    continue;
                }
                //Assumption: if it is a pseudo-element, it does not also have dynamic pseudo-class
                String pseudoElement = sel.getPseudoElement();
                if (pseudoElement != null) {
                    java.util.List l = (java.util.List) pseudoSelectors.get(pseudoElement);
                    if (l == null) {
                        l = new java.util.LinkedList();
                        pseudoSelectors.put(pseudoElement, l);
                    }
                    l.add(sel);
                    continue;
                }
                if (sel.isPseudoClass(Selector.VISITED_PSEUDOCLASS)) {
                    _visitElements.add(e);
                }
                if (sel.isPseudoClass(Selector.ACTIVE_PSEUDOCLASS)) {
                    _activeElements.add(e);
                }
                if (sel.isPseudoClass(Selector.HOVER_PSEUDOCLASS)) {
                    _hoverElements.add(e);
                }
                if (sel.isPseudoClass(Selector.FOCUS_PSEUDOCLASS)) {
                    _focusElements.add(e);
                }
                if (!sel.matchesDynamic(e, _attRes)) {
                    continue;
                }
                Selector chain = sel.getChainedSelector();
                if (chain == null) {
                    mappedSelectors.add(sel);
                } else if (chain.getAxis() == Selector.IMMEDIATE_SIBLING_AXIS) {
                    throw new RuntimeException();
                } else {
                    childMapper.axes.add(chain);
                }
            }
            resolvePseudoElements(e, pseudoSelectors);
            childMapper.style = createCascadedStyle(e, mappedSelectors);
            link(e, childMapper);
            return childMapper;
        }

    }

}

