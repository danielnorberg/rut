/*
 * Copyright (c) 2014, Oracle America, Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 *  * Neither the name of Oracle nor the names of its contributors may be used
 *    to endorse or promote products derived from this software without
 *    specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */

package dano;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.nio.CharBuffer;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Arrays.asList;

@State(Scope.Thread)
public class RegexBenchmark {

    @Param({"pre-data-with-dash-post"})
    public String haystack;

    public CharSequence haystackSequence;

    public static final Pattern PATTERN = Pattern.compile("pre-(.*)-post");

    public static final SimplePattern SIMPLE_PATTERN = SimplePattern.of("pre-<data>-post");
    public static final SimplePattern2 SIMPLE_PATTERN2 = SimplePattern2.of("pre-<data>-post");
    public static final SimplePattern2.Result SIMPLE_PATTERN2_RESULT = SIMPLE_PATTERN2.result();

    public static final RadixTrie<String> RADIX_TRIE = RadixTrie.builder(String.class)
        .insert("/usercount", "usercount")
        .insert("/users", "users")
        .insert("/users/<user>", "user")
        .insert("/users/<user>/playlistcount", "user-playlistcount")
        .insert("/users/<user>/playlists", "user-playlists")
        .insert("/users/<user>/playlists/<playlist>/itemcount", "user-playlist-itemcount")
        .insert("/users/<user>/playlists/<playlist>/items", "user-playlist-items")
        .insert("/users/<user>/playlists/<playlist>/items/<item>", "user-playlist-item")
        .insert("/users/<user>/playlists/<playlist>", "user-playlist")
        .build();

    public static final List<Pattern> URI_PATTERNS = asList(
        Pattern.compile("/usercount"),
        Pattern.compile("/users"),
        Pattern.compile("/users/([^/]*)"),
        Pattern.compile("/users/([^/]*)/playlistcount"),
        Pattern.compile("/users/([^/]*)/playlists"),
        Pattern.compile("/users/([^/]*)/playlists/([^/]*)/itemcount"),
        Pattern.compile("/users/([^/]*)/playlists/([^/]*)/items"),
        Pattern.compile("/users/([^/]*)/playlists/([^/]*)/items/([^/]*)"),
        Pattern.compile("/users/([^/]*)/playlists/([^/]*)")
    );

    public static final String RADIX_TRIE_TEST_PATH = "/users/foo-user/playlists/bar-playlist";

    private String uri;
    private List<Pattern> uriPatterns;

    @Setup
    public void setup() {
        haystackSequence = CharBuffer.wrap(haystack);
        uri = RADIX_TRIE_TEST_PATH;
        uriPatterns = URI_PATTERNS;
    }

    @Benchmark
    public String testRegex() {
        final Matcher matcher = PATTERN.matcher(haystack);
        if (!matcher.matches()) {
            throw new AssertionError("no match");
        }
        return matcher.group(1);
    }

    @Benchmark
    public String testSimple1() {
        final SimpleMatcher matcher = SIMPLE_PATTERN.matcher(haystack);
        if (!matcher.matches()) {
            throw new AssertionError("no match");
        }
        return matcher.value(0);
    }

    @Benchmark
    public CharSequence testSimple2() {
        final SimpleMatcher2 matcher = SIMPLE_PATTERN2.matcher(haystackSequence);
        if (!matcher.matches()) {
            throw new AssertionError("no match");
        }
        return matcher.value(0);
    }

    @Benchmark
    public CharSequence testSimple2ReusableOutput() {
        final boolean matches = SIMPLE_PATTERN2.match(haystackSequence, SIMPLE_PATTERN2_RESULT);
        if (!matches) {
            throw new AssertionError("no match");
        }
        return SIMPLE_PATTERN2_RESULT.value(haystackSequence, 0);
    }

    static dano.Captor Captor = new Captor(64);

    @Benchmark
    public CharSequence testRadixTrieURIRouting() {
        return RADIX_TRIE.lookup(uri, Captor);
    }

    @Benchmark
    public Pattern testRegexURIRouting() {
        for (int i = 0; i < uriPatterns.size(); i++) {
            final Pattern pattern = uriPatterns.get(i);
            if (pattern.matcher(uri).matches()) {
                return pattern;
            }
        }
        return null;
    }

    public static void main(final String... args) throws RunnerException {
        Options opt = new OptionsBuilder()
            .include(".*" + RegexBenchmark.class.getSimpleName() + ".*testRadixTrieURIRouting.*")
            .warmupIterations(5)
            .measurementIterations(20)
            .forks(5)
            .build();

        new Runner(opt).run();
    }

}
