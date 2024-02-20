/*
 * Copyright (c) 2024 Workday, Inc.
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

package com.basho.riak.client.core;

import com.basho.riak.protobuf.RiakKvPB;

public abstract class AllQuorumOptionsBuilder<T extends AllQuorumOptionsBuilder<T>>
{
    protected final RiakKvPB.RpbGetQuorumOpts.Builder getOptionBuilder
        = RiakKvPB.RpbGetQuorumOpts.newBuilder();
    protected final RiakKvPB.RpbPutQuorumOpts.Builder putOptionBuilder
        = RiakKvPB.RpbPutQuorumOpts.newBuilder();
    protected final RiakKvPB.RpbDelQuorumOpts.Builder delOptionBuilder
        = RiakKvPB.RpbDelQuorumOpts.newBuilder();

    protected abstract T self();

    /**
     * Set the r value. Individual requests (or buckets in a bucket type)
     * can override this.
     *
     * @param r the r value as an integer.
     * @return a reference to this object.
     */
    public T withGetR(int r)
    {
        getOptionBuilder.setR(r);
        return self();
    }

    /**
     * Set the pr value. Individual requests (or buckets in a bucket type)
     * can override this.
     *
     * @param pr the pr value as an integer.
     * @return a reference to this object.
     */
    public T withGetPr(int pr)
    {
        getOptionBuilder.setPr(pr);
        return self();
    }
    /**
     * Set the nVal.
     *
     * @param nVal the number of replicas.
     * @return a reference to this object.
     */
    public T withGetNVal(int nVal)
    {
        if (nVal <= 0)
        {
            throw new IllegalArgumentException("nVal must be >= 1");
        }
        getOptionBuilder.setNVal(nVal);
        return self();
    }

    /**
     * Set the dw value. Individual requests (or buckets in a bucket type)
     * can override this.
     *
     * @param dw the dw value as an integer.
     * @return a reference to this object.
     */
    public T withPutDw(int dw)
    {
        putOptionBuilder.setDw(dw);
        return self();
    }

    /**
     * @param pw the pw value as an integer.
     * @return a reference to this object.
     */
    public T withPutPw(int pw)
    {
        putOptionBuilder.setPw(pw);
        return self();
    }

    /**
     * Set the w value. Individual requests (or buckets in a bucket type)
     * can override this.
     *
     * @param w the w value as an integer.
     * @return a reference to this object.
     */
    public T withPutW(int w)
    {
        putOptionBuilder.setW(w);
        return self();
    }

    /**
     * Set the nVal.
     *
     * @param nVal the number of replicas.
     * @return a reference to this object.
     */
    public T withPutNVal(int nVal)
    {
        if (nVal <= 0)
        {
            throw new IllegalArgumentException("nVal must be >= 1");
        }
        putOptionBuilder.setNVal(nVal);
        return self();
    }

    /**
     * Set the rw value. Individual requests (or buckets in a bucket type)
     * can override this.
     *
     * @param rw the rw value as an integer.
     * @return a reference to this object.
     */
    public T withDelRw(int rw)
    {
        delOptionBuilder.setRw(rw);
        return self();
    }

    /**
     * Set the dw value. Individual requests (or buckets in a bucket type)
     * can override this.
     *
     * @param dw the dw value as an integer.
     * @return a reference to this object.
     */
    public T withDelDw(int dw)
    {
        delOptionBuilder.setDw(dw);
        return self();
    }

    /**
     * @param pw the pw value as an integer.
     * @return a reference to this object.
     */
    public T withDelPw(int pw)
    {
        delOptionBuilder.setPw(pw);
        return self();
    }

    /**
     * Set the w value. Individual requests (or buckets in a bucket type)
     * can override this.
     *
     * @param w the w value as an integer.
     * @return a reference to this object.
     */
    public T withDelW(int w)
    {
        delOptionBuilder.setW(w);
        return self();
    }

    /**
     * Set the r value. Individual requests (or buckets in a bucket type)
     * can override this.
     *
     * @param r the r value as an integer.
     * @return a reference to this object.
     */
    public T withDelR(int r)
    {
        delOptionBuilder.setR(r);
        return self();
    }

    /**
     * Set the pr value. Individual requests (or buckets in a bucket type)
     * can override this.
     *
     * @param pr the pr value as an integer.
     * @return a reference to this object.
     */
    public T withDelPr(int pr)
    {
        delOptionBuilder.setPr(pr);
        return self();
    }

    /**
     * Set the nVal.
     *
     * @param nVal the number of replicas.
     * @return a reference to this object.
     */
    public T withDelNVal(int nVal)
    {
        if (nVal <= 0)
        {
            throw new IllegalArgumentException("nVal must be >= 1");
        }
        delOptionBuilder.setNVal(nVal);
        return self();
    }


}
