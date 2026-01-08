/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.internal.inputmethod;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.inputmethod.InputMethodSubtype;

import java.util.List;

/**
 * A {@link Parcelable} container that can hold an arbitrary number of {@link InputMethodSubtype}
 * without worrying about {@link android.os.TransactionTooLargeException} when passing across
 * process boundary.
 *
 * <p>This class uses blob marshalling to securely pass InputMethodSubtype lists across IPC,
 * preventing type confusion attacks that could occur with direct Parcelable list passing.</p>
 *
 * @see Parcel#readBlob()
 * @see Parcel#writeBlob(byte[])
 * @hide
 */
public final class InputMethodSubtypeSafeList extends AbstractSafeList<InputMethodSubtype> {

    public static final Creator<InputMethodSubtypeSafeList> CREATOR = new Creator<>() {
        @Override
        public InputMethodSubtypeSafeList createFromParcel(Parcel in) {
            return new InputMethodSubtypeSafeList(in.readBlob());
        }

        @Override
        public InputMethodSubtypeSafeList[] newArray(int size) {
            return new InputMethodSubtypeSafeList[size];
        }
    };

    private InputMethodSubtypeSafeList(@Nullable byte[] buffer) {
        super(buffer);
    }

    private InputMethodSubtypeSafeList(@Nullable List<InputMethodSubtype> list) {
        super(list);
    }

    /**
     * Extracts the list of {@link InputMethodSubtype} from the given
     * {@link InputMethodSubtypeSafeList} and clears the internal buffer.
     *
     * <p>Note that each {@link InputMethodSubtype} item is guaranteed to be a copy of the original
     * {@link InputMethodSubtype} object.</p>
     *
     * <p>Any subsequent call will return an empty list.</p>
     *
     * @param from {@link InputMethodSubtypeSafeList} from which the list of
     *             {@link InputMethodSubtype} will be extracted
     * @return list of {@link InputMethodSubtype} stored in the given
     *         {@link InputMethodSubtypeSafeList}
     */
    @NonNull
    public static List<InputMethodSubtype> extractFrom(
            @Nullable InputMethodSubtypeSafeList from) {
        return AbstractSafeList.extractFrom(from, InputMethodSubtype.CREATOR);
    }

    /**
     * Creates an {@link InputMethodSubtypeSafeList} from the given list of
     * {@link InputMethodSubtype}.
     *
     * @param list list of {@link InputMethodSubtype} from which
     *             {@link InputMethodSubtypeSafeList} will be created
     * @return {@link InputMethodSubtypeSafeList} that stores the given list of
     *         {@link InputMethodSubtype}
     */
    @NonNull
    public static InputMethodSubtypeSafeList create(@Nullable List<InputMethodSubtype> list) {
        return new InputMethodSubtypeSafeList(list);
    }
}
