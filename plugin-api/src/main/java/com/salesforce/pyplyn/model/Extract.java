/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.model;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Extract data source interface
 * <p/>
 * <p/>All types that can be "extracted" should inherit this interface.
 * <p/>
 * <p/>This type implies all JSON definitions that will be deserialized into Extract will have a "format" parameter
 *   which will contain the actual subtype's name.
 * <p/>
 * <p/><b>All implementations should be {@link Serializable}!</b>
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=JsonTypeInfo.As.PROPERTY, property="format")
public interface Extract extends Serializable { }
