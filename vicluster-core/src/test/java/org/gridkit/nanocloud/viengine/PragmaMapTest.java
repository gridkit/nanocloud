package org.gridkit.nanocloud.viengine;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class PragmaMapTest {

    @Test
    public void test_simple_prop() {
        PragmaMap map = new PragmaMap();
        map.set("prop:x", "x");
        assertThat(map.<String>get("prop:x")).isEqualTo("x");
    }

    @Test
    public void test_simple_default() {
        PragmaMap map = new PragmaMap();
        map.set("default:prop:x", "x");
        assertThat(map.<String>get("prop:x")).isEqualTo("x");
    }

    @Test
    public void test_wild_card_default() {
        PragmaMap map = new PragmaMap();
        map.set("default:prop:*", "x");
        assertThat(map.<String>get("prop:x")).isEqualTo("x");
    }

    @Test
    public void test_simple_link() {
        PragmaMap map = new PragmaMap();
        map.set("prop:x", "x");
        map.link("prop:y", "prop:x");
        assertThat(map.<String>get("prop:x")).isEqualTo("x");
        assertThat(map.<String>get("prop:y")).isEqualTo("x");
        System.out.println(map.describe("prop:y"));
    }

    @Test(expected=RuntimeException.class, timeout=1000)
    public void test_cyclic_link() {
        PragmaMap map = new PragmaMap();
        map.link("prop:x", "prop:y");
        map.link("prop:y", "prop:x");
        map.get("prop:x");
    }

    @Test
    public void test_parametrized_link() {
        PragmaMap map = new PragmaMap();
        map.set("prop:link", "a");
        map.set("prop:a", "a");
        map.set("prop:b", "b");
        map.link("prop:x", "prop:${link}");
        assertThat(map.<String>get("prop:x")).isEqualTo("a");
        map.set("prop:link", "b");
        assertThat(map.<String>get("prop:x")).isEqualTo("b");
    }

    @Test
    public void test_link_to_default() {
        PragmaMap map = new PragmaMap();
        map.set("prop:link", "a");
        map.set("prop:a", "a");
        map.set("default:prop:b", "b");
        map.link("prop:x", "prop:${link}");
        assertThat(map.<String>get("prop:x")).isEqualTo("a");
        map.set("prop:link", "b");
        assertThat(map.<String>get("prop:x")).isEqualTo("b");
    }

    @Test
    public void test_default_link() {
        PragmaMap map = new PragmaMap();
        map.set("prop:link", "a");
        map.set("prop:a", "a");
        map.set("default:prop:b", "b");
        map.link("default:prop:?", "prop:${link}");
        assertThat(map.<String>get("prop:x")).isEqualTo("a");
        map.set("prop:link", "b");
        assertThat(map.<String>get("prop:x")).isEqualTo("b");
    }

    @Test
    public void test_lazy_value() {
        PragmaMap map = new PragmaMap();
        map.set("prop:link", "a");
        map.set("prop:a", "a");
        map.setLazy("prop:b", new Lazy());
        map.link("default:prop:?", "prop:${link}");
        assertThat(map.<String>get("prop:x")).isEqualTo("a");
        map.set("prop:link", "b");
        assertThat(map.<String>get("prop:x")).isEqualTo("lazy+prop:b");
    }

    @Test
    public void test_default_lazy_value() {
        PragmaMap map = new PragmaMap();
        map.set("prop:link", "a");
        map.set("prop:a", "a");
        map.link("prop:x", "prop:${link}");
        map.setLazy("default:prop:*", new Lazy());
        assertThat(map.<String>get("prop:x")).isEqualTo("a");
        map.set("prop:link", "b");
        assertThat(map.<String>get("prop:x")).isEqualTo("lazy+prop:b");
    }

    public static class Lazy implements LazyPragma {

        @Override
        public Object resolve(String key, PragmaReader context) {
            return "lazy+" + key;
        }
    }
}
