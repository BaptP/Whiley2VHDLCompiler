------------------------------------------------------------
--             VHDL file generated by                     --
--              Whiley2VHDLCompiler                       --
------------------------------------------------------------


------------------------------------------------------------
-- Entity foo
library ieee;
use ieee.std_logic_1164.all;
use ieee.numeric_std.all;

entity foo is
  port (
    foo_in_0  : in  signed(31 downto 0);
    foo_out_0 : out signed(31 downto 0)
  );
end entity foo;


architecture Behavioural of foo is
  component A
    port (
      A_in_0_k    : in  signed(31 downto 0);
      A_out_0_e   : out std_logic_vector(7 downto 0);
      A_out_0_h_k : out signed(31 downto 0);
      A_out_0_u   : out boolean
    );
  end component A;

  component bar
    port (
      bar_in_0  : in  signed(31 downto 0);
      bar_in_1  : in  signed(31 downto 0);
      bar_out_0 : out signed(31 downto 0)
    );
  end component bar;

  component swap
    port (
      swap_in_0  : in  signed(31 downto 0);
      swap_in_1  : in  signed(31 downto 0);
      swap_out_0 : out signed(31 downto 0);
      swap_out_1 : out signed(31 downto 0)
    );
  end component swap;

  signal r             : signed(31 downto 0);
  signal bar_0_in_0    : signed(31 downto 0);
  signal bar_0_in_1    : signed(31 downto 0);
  signal bar_0_out_0   : signed(31 downto 0);
  signal t_e           : std_logic_vector(7 downto 0);
  signal t_h_k         : signed(31 downto 0);
  signal t_u           : boolean;
  signal A_0_in_0_k    : signed(31 downto 0);
  signal A_0_out_0_e   : std_logic_vector(7 downto 0);
  signal A_0_out_0_h_k : signed(31 downto 0);
  signal A_0_out_0_u   : boolean;
  signal u             : signed(31 downto 0);
  signal swap_0_in_0   : signed(31 downto 0);
  signal swap_0_in_1   : signed(31 downto 0);
  signal swap_0_out_0  : signed(31 downto 0);
  signal swap_0_out_2  : signed(31 downto 0);
  signal r_2           : signed(31 downto 0);
  signal foo_in_0_2    : signed(31 downto 0);

begin
  bar_0_in_0 <= foo_in_0;
  bar_0_in_1 <= foo_in_0;
  bar_0: bar port map (
    bar_in_0  => bar_0_in_0,
    bar_in_1  => bar_0_in_1,
    bar_out_0 => bar_0_out_0
  );

  r <= bar_0_out_0;

  A_0_in_0_k <= foo_in_0;
  A_0: A port map (
    A_in_0_k    => A_0_in_0_k,
    A_out_0_e   => A_0_out_0_e,
    A_out_0_h_k => A_0_out_0_h_k,
    A_out_0_u   => A_0_out_0_u
  );

  t_e   <= A_0_out_0_e;
  t_h_k <= A_0_out_0_h_k;
  t_u   <= A_0_out_0_u;

  swap_0_in_0 <= t_h_k;
  swap_0_in_1 <= r - to_signed(3, 32);
  swap_0: swap port map (
    swap_in_0  => swap_0_in_0,
    swap_in_1  => swap_0_in_1,
    swap_out_0 => swap_0_out_0,
    swap_out_1 => swap_0_out_2
  );

  u <= swap_0_out_0;
  r_2 <= swap_0_out_2;
  foo_in_0_2 <= foo_in_0 + to_signed(2, 32);

  foo_out_0 <= u + r_2 + foo_in_0_2 - to_signed(2, 32);
end architecture Behavioural;
-- Entity foo
------------------------------------------------------------



------------------------------------------------------------
-- Entity bar
library ieee;
use ieee.std_logic_1164.all;
use ieee.numeric_std.all;

entity bar is
  port (
    bar_in_0  : in  signed(31 downto 0);
    bar_in_1  : in  signed(31 downto 0);
    bar_out_0 : out signed(31 downto 0)
  );
end entity bar;


architecture Behavioural of bar is

  signal a : signed(31 downto 0);
  signal b : signed(31 downto 0);

begin
  a <= bar_in_0 + to_signed(2, 32);

  b <= a - to_signed(4, 32);

  bar_out_0 <= bar_in_1 + b + a;
end architecture Behavioural;
-- Entity bar
------------------------------------------------------------



------------------------------------------------------------
-- Entity swap
library ieee;
use ieee.std_logic_1164.all;
use ieee.numeric_std.all;

entity swap is
  port (
    swap_in_0  : in  signed(31 downto 0);
    swap_in_1  : in  signed(31 downto 0);
    swap_out_0 : out signed(31 downto 0);
    swap_out_1 : out signed(31 downto 0)
  );
end entity swap;


architecture Behavioural of swap is


begin
  swap_out_0 <= swap_in_1;
  swap_out_1 <= swap_in_0;
end architecture Behavioural;
-- Entity swap
------------------------------------------------------------



------------------------------------------------------------
-- Entity A
library ieee;
use ieee.std_logic_1164.all;
use ieee.numeric_std.all;

entity A is
  port (
    A_in_0_k    : in  signed(31 downto 0);
    A_out_0_e   : out std_logic_vector(7 downto 0);
    A_out_0_h_k : out signed(31 downto 0);
    A_out_0_u   : out boolean
  );
end entity A;


architecture Behavioural of A is

  signal s_e     : std_logic_vector(7 downto 0);
  signal s_h_k   : signed(31 downto 0);
  signal s_u     : boolean;
  signal s_h_k_2 : signed(31 downto 0);

begin
  s_e   <= "00000000";
  s_h_k <= A_in_0_k;
  s_u   <= false;

  s_h_k_2 <= s_h_k + to_signed(3, 32);

  A_out_0_e   <= s_e;
  A_out_0_h_k <= s_h_k_2;
  A_out_0_u   <= s_u;
end architecture Behavioural;
-- Entity A
------------------------------------------------------------



